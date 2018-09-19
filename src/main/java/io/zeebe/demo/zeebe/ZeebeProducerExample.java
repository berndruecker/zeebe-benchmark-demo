package io.zeebe.demo.zeebe;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.zeebe.gateway.ZeebeClient;
import io.zeebe.gateway.api.events.DeploymentEvent;
import io.zeebe.model.bpmn.Bpmn;

public class ZeebeProducerExample {
  
  public static void main(String[] args) {

    final ZeebeClient client = ZeebeClient.newClient();

    long key = deploy(client);
    
    System.out.println("deployed - starting instances");

    final AtomicLong counter = new AtomicLong();
    new Timer(true).scheduleAtFixedRate(new TimerTask() {

      @Override
      public void run() {
        long started = counter.getAndSet(0);

        System.out.println(started);
      }
    }, 1000, 1000);

    final long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30);
    while (endTime > System.currentTimeMillis()) {
      client.topicClient().workflowClient() //
        .newCreateInstanceCommand().bpmnProcessId("demo").latestVersion().payload("{}") //
        .send();
      counter.incrementAndGet();
    }

    client.close();

  }

  private static long deploy(final ZeebeClient client) {
    final DeploymentEvent deploymentEvent = client.topicClient().workflowClient().newDeployCommand()
        .addWorkflowModel( //
            Bpmn.createExecutableProcess("demo").startEvent().endEvent().done(), //
            "demo.bpmn")//
        .send().join();

    long key = deploymentEvent.getKey();
    return key;
  }

}
