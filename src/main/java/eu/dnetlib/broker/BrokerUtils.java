package eu.dnetlib.broker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
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
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

@Component
public class BrokerUtils {

	private static final Logger log = LoggerFactory.getLogger(BrokerUtils.class);

	private static final HttpClient client = HttpClientBuilder.create().build();

	public List<String> listSubscriptions(final URL baseUrl, final String email) throws Exception {
		final String url = baseUrl + "/subscriptions?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8.name());
		log.info("Performing HTTP GET for subscriptions: " + url);

		final HttpGet request = new HttpGet(url);
		request.addHeader("accept", "application/json");
		final HttpResponse response = client.execute(request);
		final String json = IOUtils.toString(response.getEntity().getContent());
		log.info("Found subscriptions: " + json);

		final JSONArray array = new JSONArray(json);

		System.out.println(String.format("Found %d subscription(s):", array.length()));

		final List<String> res = new ArrayList<>();
		for (int i = 0; i < array.length(); i++) {
			final JSONObject object = array.getJSONObject(i);

			final String suscrId = object.getString("subscriptionId");
			final String topic = object.getString("topic");
			final String ds = extractDsName(object.getJSONArray("conditionsAsList"));

			System.out.println(String.format(" - %s (TOPIC: %s, Datasource: %s)", suscrId, topic, ds));

			res.add(suscrId);
		}
		System.out.println();

		return res;
	}

	public void downloadEvents(final URL baseUrl, final String subscrId, final File outputDir, final boolean gzip) throws Exception {
		final String fp = String.format(gzip ? "%s/%s.json.gz" : "%s/%s.json", outputDir.getAbsolutePath(), subscrId);

		System.out.print("Saving file " + fp + ": ");
		System.out.flush();

		if (gzip) {
			try (final FileOutputStream fos = new FileOutputStream(fp); final Writer w = new OutputStreamWriter(new GZIPOutputStream(fos), "UTF-8")) {
				writeEvents(baseUrl, subscrId, w);
			}
		} else {
			try (final FileWriter fw = new FileWriter(fp)) {
				writeEvents(baseUrl, subscrId, fw);
			}
		}
		System.out.println();
	}

	private void writeEvents(final URL baseUrl, final String subscrId, final Writer file) throws Exception {

		String url = baseUrl + "/scroll/notifications/bySubscriptionId/" + URLEncoder.encode(subscrId, StandardCharsets.UTF_8.name());
		boolean notCompleted = false;

		do {
			log.info("Performing HTTP GET for notifications: " + url);
			final HttpGet request = new HttpGet(url);
			request.addHeader("accept", "application/json");
			final HttpResponse response = client.execute(request);
			final String json = IOUtils.toString(response.getEntity().getContent());
			final JSONObject data = new JSONObject(json);

			final JSONArray values = data.getJSONArray("values");
			for (int i = 0; i < values.length(); i++) {
				file.append(values.getJSONObject(i).toString());
				file.append("\n");
			}

			notCompleted = !data.getBoolean("completed");
			url = baseUrl + "/scroll/notifications/" + data.getString("id");

			System.out.print(".");
			System.out.flush();
		} while (notCompleted);
	}

	private String extractDsName(final JSONArray conds) {
		try {
			for (int i = 0; i < conds.length(); i++) {
				final JSONObject object = conds.getJSONObject(i);
				if (object.getString("field").equals("targetDatasourceName")) { return object.getJSONArray("listParams").getJSONObject(0).getString("value"); }
			}
		} catch (final JSONException e) {
			log.warn(e.getMessage());
		}

		return "";
	}

}
