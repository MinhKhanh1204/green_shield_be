package com.chatbox.chatbox;

import com.chatbox.chatbox.service.KnowledgeLoaderService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeLoaderServiceTests {

    private final KnowledgeLoaderService knowledgeLoaderService = new KnowledgeLoaderService();

    @Test
    void linkQuestionReturnsOfficialRoutesFromTheLinkResource() {
        String knowledge = knowledgeLoaderService.loadKnowledge(
                "Cho tôi link truy cập Lab 3D và bản đồ vùng nguyên liệu"
        );

        assertThat(knowledge)
                .contains("00-lien-ket-chinh-thuc.md")
                .contains("https://greenshieldmekong.com/custom-bag")
                .contains("https://greenshieldmekong.com/map")
                .hasSizeLessThanOrEqualTo(12_000);
    }

    @Test
    void financialQuestionReturnsInvestmentSectionsWithoutLoadingAllFiles() {
        String knowledge = knowledgeLoaderService.loadKnowledge(
                "Nguồn vốn đầu tư và kế hoạch sử dụng 500 triệu như thế nào?"
        );

        assertThat(knowledge)
                .containsAnyOf("05-dau-tu.md", "09-du-lieu-hinh-anh.md")
                .contains("500.000.000 VNĐ")
                .contains("Nguồn vốn đầu tư")
                .hasSizeLessThanOrEqualTo(12_000);
    }

    @Test
    void productQuestionReturnsPricingAndMaterialKnowledge() {
        String knowledge = knowledgeLoaderService.loadKnowledge(
                "Giá và vật liệu của hộp đựng trái cây"
        );

        assertThat(knowledge)
                .contains("02-san-pham.md")
                .containsAnyOf("Hộp đựng trái cây", "Hộp trái cây")
                .containsAnyOf("2,500 ₫", "2.500 VNĐ")
                .hasSizeLessThanOrEqualTo(12_000);
    }

    @Test
    void supportQuestionDoesNotInventAnUnpublishedShippingPolicy() {
        String knowledge = knowledgeLoaderService.loadKnowledge(
                "Thời gian shipping và giao hàng quốc tế là bao lâu?"
        );

        assertThat(knowledge)
                .contains("10-ho-tro-khach-hang.md")
                .contains("chưa công bố")
                .contains("Không đưa ra mốc thời gian")
                .hasSizeLessThanOrEqualTo(12_000);
    }
}
