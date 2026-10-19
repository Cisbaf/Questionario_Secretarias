package questionario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class QuestionarioApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuestionarioApplication.class, args);
    }
}
