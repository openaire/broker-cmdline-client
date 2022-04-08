package eu.dnetlib.broker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class BrokerClient {

	private static final Logger log = LoggerFactory.getLogger(BrokerClient.class);

	private static final HttpClient client = HttpClientBuilder.create().build();

	public List<String> listSubscriptions(final URL baseUrl, final String email) throws Exception {
		final String url = baseUrl + "/subscriptions?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8.name());
		log.debug("Performing HTTP GET for subscriptions: " + url);

		final HttpGet request = new HttpGet(url);
		request.addHeader("accept", "application/json");
		final HttpResponse response = client.execute(request);
		final String json = IOUtils.toString(response.getEntity().getContent());
		log.debug("Found subscriptions: " + json);

		final JsonArray array = JsonParser.parseString(json).getAsJsonArray();

		log.info(String.format("Found %d subscription(s)", array.size()));

		final List<String> res = new ArrayList<>();
		for (int i = 0; i < array.size(); i++) {
			final JsonObject object = array.get(i).getAsJsonObject();

			final String suscrId = object.get("subscriptionId").getAsString();
			final String topic = object.get("topic").getAsString();
			final String ds = extractDsName(object.get("conditionsAsList").getAsJsonArray());

			log.info(String.format(" - %s (TOPIC: %s, Datasource: %s)", suscrId, topic, ds));

			res.add(suscrId);
		}

		return res;
	}

	public void downloadEvents(final URL baseUrl, final String subscrId, final File outputDir, final boolean gzip) throws Exception {
		final String fp = String.format(gzip ? "%s/%s.json.gz" : "%s/%s.json", outputDir.getAbsolutePath(), subscrId);

		log.info("Saving file " + fp + ": ");

		if (gzip) {
			try (final FileOutputStream fos = new FileOutputStream(fp); final Writer w = new OutputStreamWriter(new GZIPOutputStream(fos), "UTF-8")) {
				writeEvents(baseUrl, subscrId, w);
			}
		} else {
			try (final FileWriter fw = new FileWriter(fp)) {
				writeEvents(baseUrl, subscrId, fw);
			}
		}

	}

	public void downloadEvents(final URL baseUrl, final String subscrId, final OutputStream outputStream) throws Exception {
		try (Writer writer = new OutputStreamWriter(outputStream)) {
			writeEvents(baseUrl, subscrId, writer);
		}
	}

	private void writeEvents(final URL baseUrl, final String subscrId, final Writer file) throws Exception {

		String url = baseUrl + "/scroll/notifications/bySubscriptionId/" + URLEncoder.encode(subscrId, StandardCharsets.UTF_8.name());
		boolean notCompleted = false;
		boolean first = true;

		file.append("[\n");
		do {
			log.debug("Performing HTTP GET for notifications: " + url);
			final HttpGet request = new HttpGet(url);
			request.addHeader("accept", "application/json");
			final HttpResponse response = client.execute(request);
			final String json = IOUtils.toString(response.getEntity().getContent());
			final JsonObject data = JsonParser.parseString(json).getAsJsonObject();

			final JsonArray values = data.get("values").getAsJsonArray();
			for (int i = 0; i < values.size(); i++) {
				if (first) {
					first = false;
				} else {
					file.append(",\n");
				}
				file.append(values.get(i).getAsJsonObject().toString());
			}

			notCompleted = !data.get("completed").getAsBoolean();
			url = baseUrl + "/scroll/notifications/" + data.get("id").getAsString();

		} while (notCompleted);

		file.append("\n]\n");
	}

	private String extractDsName(final JsonArray conds) {
		try {
			for (int i = 0; i < conds.size(); i++) {
				final JsonObject object = conds.get(i).getAsJsonObject();
				if (object.get("field").getAsString().equals("targetDatasourceName")) {
					return object.get("listParams").getAsJsonArray().get(0).getAsJsonObject().get("value").getAsString();
				}
			}
		} catch (final Throwable e) {
			log.warn(e.getMessage());
		}

		return "";
	}

}
