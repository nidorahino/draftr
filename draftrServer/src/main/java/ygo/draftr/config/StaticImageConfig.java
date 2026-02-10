package ygo.draftr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class StaticImageConfig implements WebMvcConfigurer {

    @Value("${app.images.dir:./local-images/ygo/cards}")
    private String imagesDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path path = Paths.get(imagesDir).toAbsolutePath().normalize();
        String location = "file:" + path.toString() + "/";

        registry.addResourceHandler("/images/cards/**")
                .addResourceLocations(location);
    }
}