package com.chatbox.chatbox;

import com.chatbox.chatbox.config.TopicConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TopicConfigTests {

    private final TopicConfig topicConfig = new TopicConfig();

    @Test
    void exposesVietnameseTopicsMatchingTheKnowledgeGroups() {
        assertThat(topicConfig.getAllTopics())
                .containsKeys(
                        "tong_quan_du_an",
                        "san_pham",
                        "vat_lieu_cong_nghe",
                        "thi_truong_khach_hang",
                        "esg_ben_vung",
                        "lien_ket_lien_he",
                        "ho_tro_khach_hang"
                )
                .doesNotContainKeys("dau_tu", "tai_chinh", "tiep_thi_ban_hang")
                .allSatisfy((key, description) -> assertThat(description)
                        .isNotBlank()
                        .doesNotContain("About GreenShield", "How to place", "Payment methods"));
    }

    @Test
    void keepsLegacyTopicKeysAsAliases() {
        assertThat(topicConfig.getTopicContext("products"))
                .isEqualTo(topicConfig.getTopicContext("san_pham"));
        assertThat(topicConfig.getTopicContext("shipping"))
                .isEqualTo(topicConfig.getTopicContext("ho_tro_khach_hang"));
        assertThat(topicConfig.getTopicContext("contact"))
                .isEqualTo(topicConfig.getTopicContext("lien_ket_lien_he"));
    }

    @Test
    void returnsLocalizedTopicLabels() {
        assertThat(topicConfig.getTopicLabel("san_pham", "vi")).isEqualTo("Sản phẩm");
        assertThat(topicConfig.getTopicLabel("san_pham", "en")).isEqualTo("Products");
        assertThat(topicConfig.getTopicLabel("products", "en-US")).isEqualTo("Products");
    }
}
