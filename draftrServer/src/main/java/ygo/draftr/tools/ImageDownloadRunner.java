package ygo.draftr.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import ygo.draftr.tools.ygopro.CardDto;
import ygo.draftr.tools.ygopro.CardImageDto;
import ygo.draftr.tools.ygopro.CardInfoResponse;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ImageDownloadRunner implements CommandLineRunner {

    private final ObjectMapper mapper;

    @Value("${app.images.enabled:false}")
    private boolean enabled;

    @Value("${app.images.dir:./local-images/ygo/cards}")
    private String outDir;

    @Value("${app.images.per-second:4}")
    private int perSecond;

    @Value("${app.images.limit:0}")
    private int limit; // 0 = no limit

    public ImageDownloadRunner(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!enabled) return;

        // Defensive throttle: never go faster than 2/sec by accident
        int safePerSecond = Math.max(2, perSecond);
        long delayMs = Math.max(250L, 1000L / safePerSecond);

        Path base = Paths.get(outDir).toAbsolutePath();
        Files.createDirectories(base);

        CardInfoResponse response;
        try (InputStream is = new ClassPathResource("ygo/cardinfo.json").getInputStream()) {
            response = mapper.readValue(is, CardInfoResponse.class);
        }

        List<CardDto> cards = response.data();
        if (cards == null || cards.isEmpty()) {
            System.out.println("No cards found in ygo/cardinfo.json");
            return;
        }

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        Set<Long> seenIds = new HashSet<>();
        int downloaded = 0;
        int skippedExisting = 0;
        int skippedDupInFile = 0;
        int failed = 0;

        int processed = 0;

        for (CardDto card : cards) {
            if (card == null) continue;

            long id = card.id();

            // De-dupe by id in case your file contains duplicates
            if (!seenIds.add(id)) {
                skippedDupInFile++;
                continue;
            }

            // Only download default artwork (index 0)
            if (card.card_images() == null || card.card_images().isEmpty()) continue;
            CardImageDto img = card.card_images().get(0);
            if (img == null || img.image_url() == null || img.image_url().isBlank()) continue;

            Path dest = base.resolve(id + ".jpg");

            // Skip if already downloaded
            if (Files.exists(dest) && Files.size(dest) > 0) {
                skippedExisting++;
                continue;
            }

            boolean ok = downloadOne(client, img.image_url(), dest);
            if (ok) downloaded++;
            else failed++;

            processed++;

            if (limit > 0 && processed >= limit) break;

            // Throttle to be nice (avoid blacklist risk)
            Thread.sleep(delayMs);
        }

        System.out.println("Image download complete.");
        System.out.println("Downloaded: " + downloaded);
        System.out.println("Skipped existing: " + skippedExisting);
        System.out.println("Skipped dup IDs in file: " + skippedDupInFile);
        System.out.println("Failed: " + failed);
        System.out.println("Output folder: " + base);
    }

    private boolean downloadOne(HttpClient client, String url, Path dest) {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .header("User-Agent", "DraftrDev/1.0 (local image downloader)")
                    .GET()
                    .build();

            HttpResponse<byte[]> res = client.send(req, HttpResponse.BodyHandlers.ofByteArray());

            if (res.statusCode() >= 400 || res.body() == null || res.body().length == 0) {
                System.err.println("FAIL " + res.statusCode() + " " + url);
                return false;
            }

            Path tmp = dest.resolveSibling(dest.getFileName().toString() + ".tmp");
            Files.write(tmp, res.body(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Files.move(tmp, dest, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            return true;

        } catch (Exception ex) {
            System.err.println("ERROR downloading: " + url + " | " + ex.getMessage());
            return false;
        }
    }
}
