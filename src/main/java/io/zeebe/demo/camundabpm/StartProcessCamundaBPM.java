package io.zeebe.demo.camundabpm;

import java.io.IOException;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class StartProcessCamundaBPM
{
	private static final int THREAD_COUNT = 8;

	public static void main(String[] args) throws IOException, InterruptedException
	{

		try (CloseableHttpClient httpclient = HttpClients.createDefault())
		{
			deploy(httpclient);
			
			final AtomicLong processesStarted = new AtomicLong();

			new Timer(true)
				.scheduleAtFixedRate(new TimerTask()
				{
					
					@Override
					public void run()
					{
						long started = processesStarted.getAndSet(0);
						
						System.out.println(started);
					}
				}, 1000, 1000);
			
			final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
			
			for (int i = 0; i < THREAD_COUNT; i++)
			{
				executorService.submit(() ->
				{
					while (true)
					{
						try
						{
							startProcess(httpclient);
							processesStarted.incrementAndGet();
						}
						catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
			}

			Thread.sleep(TimeUnit.SECONDS.toMillis(30));
			
			System.exit(0);
			
		}
		
	}

	private static void startProcess(CloseableHttpClient httpclient) throws IOException
	{
		HttpPost httpPost = new HttpPost("http://localhost:8080/engine-rest/process-definition/key/sendPush/start");
		httpPost.setHeader("Content-Type", "application/json");
		httpPost.setHeader("Accept", "application/json");
		httpPost.setEntity(new StringEntity("{}"));
		
		try (CloseableHttpResponse response = httpclient.execute(httpPost))
		{
			EntityUtils.consume(response.getEntity());

			if (response.getStatusLine().getStatusCode() != 200)
			{
				throw new RuntimeException("Could not start process instance");
			}
		}		
	}

	private static void deploy(CloseableHttpClient httpclient) throws IOException
	{
		URL processResource = StartProcessCamundaBPM.class.getResource("/process1.bpmn");
		
		HttpPost httpPost = new HttpPost("http://localhost:8090/engine-rest/deployment/create");
		
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.addTextBody("deployment-name", "rest-deployment", ContentType.TEXT_PLAIN);
		builder.addTextBody("enable-duplicate-filtering	", "true", ContentType.TEXT_PLAIN);

		
		builder.addBinaryBody(
		    "process",
		    processResource.openStream(),
		    ContentType.APPLICATION_OCTET_STREAM,
		    "test-process-bpmn"
		);

		HttpEntity multipart = builder.build();
		
		httpPost.setEntity(multipart);
		
		try (CloseableHttpResponse response = httpclient.execute(httpPost))
		{
			EntityUtils.consume(response.getEntity());
			
			if(response.getStatusLine().getStatusCode() != 200)
			{
				throw new RuntimeException("Could not deploy process.");
			}
		}
	}

}
