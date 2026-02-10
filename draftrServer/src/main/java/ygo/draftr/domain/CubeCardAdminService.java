package ygo.draftr.domain;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ygo.draftr.data.CubeCardRepository;
import ygo.draftr.models.CubeCard;

@Service
public class CubeCardAdminService {

    private final CubeService cubeService;
    private final CubeCardRepository cubeCardRepo;

    public CubeCardAdminService(CubeService cubeService, CubeCardRepository cubeCardRepo) {
        this.cubeService = cubeService;
        this.cubeCardRepo = cubeCardRepo;
    }

    @Transactional
    public CubeCard updateMaxQty(Long cubeId, Long cardId, int maxQty, Long actorUserId) {
        cubeService.requireMembership(cubeId, actorUserId);

        if (maxQty < 1 || maxQty > 20) {
            throw new IllegalArgumentException("maxQty must be between 1 and 20");
        }

        CubeCard cc = cubeCardRepo.findByCubeIdAndCardId(cubeId, cardId)
                .orElseThrow(() -> new IllegalArgumentException("card not found in cube"));

        cc.setMaxQty(maxQty);
        return cubeCardRepo.save(cc);
    }

    @Transactional
    public void removeCard(Long cubeId, Long cardId, Long actorUserId) {
        cubeService.requireMembership(cubeId, actorUserId);

        CubeCard cc = cubeCardRepo.findByCubeIdAndCardId(cubeId, cardId)
                .orElseThrow(() -> new IllegalArgumentException("card not found in cube"));

        cubeCardRepo.delete(cc);
    }
}
