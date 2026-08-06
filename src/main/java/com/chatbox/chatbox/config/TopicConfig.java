package com.chatbox.chatbox.config;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class TopicConfig {

    private static final String DEFAULT_TOPIC = "hoi_dap_chung";

    private final Map<String, String> topicMap = new LinkedHashMap<>();
    private final Map<String, String> topicLabelsVi = new LinkedHashMap<>();
    private final Map<String, String> topicLabelsEn = new LinkedHashMap<>();
    private final Map<String, String> topicAliases = new LinkedHashMap<>();

    public TopicConfig() {
        topicMap.put(
                "tong_quan_du_an",
                "Tổng quan dự án, ý tưởng hình thành, sứ mệnh, tầm nhìn và định hướng phát triển GreenShield Mekong."
        );
        topicMap.put(
                "san_pham",
                "Danh mục sản phẩm, đặc điểm, vật liệu, chi phí sản xuất, giá bán và biên lợi nhuận."
        );
        topicMap.put(
                "vat_lieu_cong_nghe",
                "Vật liệu lục bình, lá sen, quy trình kỹ thuật, AI, truy xuất nguồn gốc, Lab 3D và bản đồ nguyên liệu."
        );
        topicMap.put(
                "thi_truong_khach_hang",
                "Xu hướng thị trường, phân khúc khách hàng B2B/B2C, đối thủ cạnh tranh và kênh phân phối."
        );
        topicMap.put(
                "esg_ben_vung",
                "Giá trị môi trường, xã hội, quản trị ESG, kinh tế tuần hoàn, Net Zero và tác động cộng đồng."
        );
        topicMap.put(
                "rui_ro_thoai_von",
                "Những rủi ro chính của dự án, biện pháp ứng phó và chiến lược thoái vốn."
        );
        topicMap.put(
                "doi_ngu",
                "Thành viên dự án, đội ngũ quản lý, vai trò chuyên môn và cơ cấu tổ chức."
        );
        topicMap.put(
                "lien_ket_lien_he",
                "Website, đường dẫn sản phẩm, tính năng trực tuyến, email, điện thoại và mạng xã hội chính thức."
        );
        topicMap.put(
                "ho_tro_khach_hang",
                "Hỗ trợ đặt hàng, giao hàng, thanh toán và phản hồi theo thông tin GreenShield đã công bố; không tự tạo chính sách."
        );
        topicMap.put(
                "ho_so_thanh_tich",
                "Tài liệu tham khảo, kiểm định, hợp tác doanh nghiệp, cuộc thi, truyền thông và thành tích dự án."
        );
        topicMap.put(
                DEFAULT_TOPIC,
                "Hỏi đáp chung về GreenShield Mekong dựa trên hồ sơ dự án và các thông tin chính thức."
        );

        topicLabelsVi.put("tong_quan_du_an", "Tổng quan dự án");
        topicLabelsVi.put("san_pham", "Sản phẩm");
        topicLabelsVi.put("vat_lieu_cong_nghe", "Vật liệu và công nghệ");
        topicLabelsVi.put("thi_truong_khach_hang", "Thị trường và khách hàng");
        topicLabelsVi.put("esg_ben_vung", "ESG và bền vững");
        topicLabelsVi.put("rui_ro_thoai_von", "Rủi ro và thoái vốn");
        topicLabelsVi.put("doi_ngu", "Đội ngũ");
        topicLabelsVi.put("lien_ket_lien_he", "Liên kết và liên hệ");
        topicLabelsVi.put("ho_tro_khach_hang", "Hỗ trợ khách hàng");
        topicLabelsVi.put("ho_so_thanh_tich", "Hồ sơ và thành tích");
        topicLabelsVi.put(DEFAULT_TOPIC, "Hỏi đáp chung");

        topicLabelsEn.put("tong_quan_du_an", "Project overview");
        topicLabelsEn.put("san_pham", "Products");
        topicLabelsEn.put("vat_lieu_cong_nghe", "Materials and technology");
        topicLabelsEn.put("thi_truong_khach_hang", "Market and customers");
        topicLabelsEn.put("esg_ben_vung", "ESG and sustainability");
        topicLabelsEn.put("rui_ro_thoai_von", "Risks and exit strategy");
        topicLabelsEn.put("doi_ngu", "Team");
        topicLabelsEn.put("lien_ket_lien_he", "Links and contact");
        topicLabelsEn.put("ho_tro_khach_hang", "Customer support");
        topicLabelsEn.put("ho_so_thanh_tich", "Profile and achievements");
        topicLabelsEn.put(DEFAULT_TOPIC, "General questions");

        topicAliases.put("company", "tong_quan_du_an");
        topicAliases.put("products", "san_pham");
        topicAliases.put("contact", "lien_ket_lien_he");
        topicAliases.put("sustainability", "esg_ben_vung");
        topicAliases.put("order", "ho_tro_khach_hang");
        topicAliases.put("shipping", "ho_tro_khach_hang");
        topicAliases.put("payment", "ho_tro_khach_hang");
        topicAliases.put("feedback", "ho_tro_khach_hang");
        topicAliases.put("default", DEFAULT_TOPIC);
    }

    public String getTopicContext(String topic) {
        String canonicalTopic = getCanonicalTopic(topic);
        return topicMap.getOrDefault(canonicalTopic, topicMap.get(DEFAULT_TOPIC));
    }

    public Map<String, String> getAllTopics() {
        return Collections.unmodifiableMap(topicMap);
    }

    public String getTopicLabel(String topic, String language) {
        String canonicalTopic = getCanonicalTopic(topic);
        Map<String, String> labels = language != null && language.toLowerCase(Locale.ROOT).startsWith("en")
                ? topicLabelsEn
                : topicLabelsVi;
        return labels.getOrDefault(canonicalTopic, labels.get(DEFAULT_TOPIC));
    }

    private String getCanonicalTopic(String topic) {
        String normalizedTopic = topic == null ? "" : topic.trim().toLowerCase(Locale.ROOT);
        return topicAliases.getOrDefault(normalizedTopic, normalizedTopic);
    }
}
