package net.studio1122.threadtest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/test")
    public String test() throws InterruptedException {
        Thread.sleep(10000); // 10초 대기
        return "Done";
    }

}
