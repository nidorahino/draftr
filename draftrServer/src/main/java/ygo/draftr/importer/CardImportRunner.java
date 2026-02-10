package ygo.draftr.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ygo.draftr.data.CardRepository;
import ygo.draftr.importer.dto.CardDto;
import ygo.draftr.importer.dto.CardInfoResponse;
import ygo.draftr.models.Card;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class CardImportRunner implements CommandLineRunner {

    private final ObjectMapper mapper;
    private final CardRepository repo;

    @Value("${app.import.cards.enabled:false}")
    private boolean enabled;

    @Value("${app.import.cards.skip-if-data:true}")
    private boolean skipIfData;

    public CardImportRunner(ObjectMapper mapper, CardRepository repo) {
        this.mapper = mapper;
        this.repo = repo;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!enabled) return;

        if (skipIfData && repo.count() > 0) {
            System.out.println("Card import skipped (card table already has data).");
            return;
        }

        CardInfoResponse response;
        try (InputStream is = new ClassPathResource("ygo/cardinfo.json").getInputStream()) {
            response = mapper.readValue(is, CardInfoResponse.class);
        }

        List<CardDto> cards = response.data();
        if (cards == null || cards.isEmpty()) {
            System.out.println("No cards found in ygo/cardinfo.json");
            return;
        }

        Set<Long> seenIds = new HashSet<>();
        int imported = 0;
        int skippedDupInFile = 0;

        for (CardDto dto : cards) {
            if (dto == null) continue;

            long id = dto.id();

            // guard against duplicate entries in the JSON file
            if (!seenIds.add(id)) {
                skippedDupInFile++;
                continue;
            }

            Card card = new Card();
            card.setId(id);
            card.setName(dto.name());
            card.setCardType(dto.type());
            card.setHumanReadableCardType(dto.humanReadableCardType());
            card.setFrameType(dto.frameType());
            card.setDescription(dto.desc());
            card.setRace(dto.race());
            card.setArchetype(dto.archetype());
            card.setTypeline(dto.typeline() == null ? null : String.join(", ", dto.typeline()));
            card.setAttribute(dto.attribute());
            card.setLevel(dto.level());
            card.setAtk(dto.atk());
            card.setDef(dto.def());
            card.setYgoprodeckUrl(dto.ygoprodeck_url());

            card.setImageUrl("cards/" + id + ".jpg");

            // save = insert or update by primary key (card_id)
            repo.save(card);
            imported++;
        }

        System.out.println("Card import complete. Imported/Updated=" + imported
                + " | Skipped duplicate IDs in file=" + skippedDupInFile);
    }
}
