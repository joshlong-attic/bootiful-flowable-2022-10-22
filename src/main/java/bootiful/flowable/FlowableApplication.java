package bootiful.flowable;


import liquibase.pro.packaged.P;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@SpringBootApplication
public class FlowableApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlowableApplication.class, args);
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> listener(RuntimeService runtimeService) {
        return event -> {
            var magnumPI = runtimeService.startProcessInstanceByKey("oneTaskProcess");
            System.out.println("process instance id: " + magnumPI.getId());
        };
    }

    @Bean
    Printer printer() {
        return new Printer();
    }


}

class Printer {

    public void print() {
        System.out.println("hello, Luke!");
    }
}

@ResponseBody
@Controller
class TaskController {

    private final TaskService taskService;
    private final RuntimeService runtimeService;

    TaskController(TaskService taskService, RuntimeService runtimeService) {
        this.taskService = taskService;
        this.runtimeService = runtimeService;
    }

    @GetMapping("/processes")
    Map<String, Object> processes() {
        var pis = this.runtimeService.createProcessInstanceQuery()
                .list();
        for (var pi : pis) {
            var name = pi.getProcessDefinitionName();
            var id = pi.getId();
            var ended = pi.isEnded();
            return Map.of("name", (Object) name, "id", id, "ended", (Object) ended);
        }
        return Map.of();
    }

    @GetMapping("/flow/{user}")
    void complete(@PathVariable String user) {
        taskService
                .createTaskQuery()
                .taskAssignee(user)
                .active()
                .list()
                .forEach(task -> {
                    taskService.claim(task.getId(), user);
                    try {
                        System.out.println("finishing task (" + task.getId() + ") for user (" +
                                task.getAssignee() + ")");
                        Thread.sleep(1_000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    taskService.complete(task.getId());
                });
    }
}
