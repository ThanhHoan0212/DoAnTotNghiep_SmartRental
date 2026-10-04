package com.phongtro.backend.repository.specification;

import com.phongtro.backend.dto.request.RoomSearchRequest;
import com.phongtro.backend.entity.Amenity;
import com.phongtro.backend.entity.Room;
import com.phongtro.backend.entity.RoomStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class RoomSpecification {

    private RoomSpecification() {
        // Utility class
    }

    public static Specification<Room> buildSearchSpecification(RoomSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Tin đã duyệt vẫn công khai khi đang giữ chỗ hoặc đã cho thuê.
            predicates.add(root.get("status").in(RoomStatus.APPROVED, RoomStatus.RESERVED, RoomStatus.RENTED));

            // 2. Lọc theo từ khóa (khớp trong tiêu đề, mô tả, địa chỉ, phường, quận)
            if (request.getKeyword() != null && !request.getKeyword().trim().isBlank()) {
                String kw = "%" + request.getKeyword().trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), kw);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), kw);
                Predicate addrMatch = cb.like(cb.lower(root.get("address")), kw);
                Predicate wardMatch = cb.like(cb.lower(root.get("ward")), kw);
                Predicate distMatch = cb.like(cb.lower(root.get("district")), kw);

                predicates.add(cb.or(titleMatch, descMatch, addrMatch, wardMatch, distMatch));
            }

            // 3. Lọc theo Quận / Huyện
            if (request.getDistrict() != null && !request.getDistrict().trim().isBlank()) {
                String dist = "%" + request.getDistrict().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("district")), dist));
            }

            // 4. Lọc theo Phường / Xã
            if (request.getWard() != null && !request.getWard().trim().isBlank()) {
                String ward = "%" + request.getWard().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("ward")), ward));
            }

            // 5. Lọc theo Tỉnh / Thành phố
            if (request.getCity() != null && !request.getCity().trim().isBlank()) {
                String city = "%" + request.getCity().trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("city")), city));
            }

            // 6. Lọc theo khoảng giá thuê (minPrice - maxPrice)
            if (request.getMinPrice() != null && request.getMinPrice() > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), request.getMinPrice()));
            }
            if (request.getMaxPrice() != null && request.getMaxPrice() > 0) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), request.getMaxPrice()));
            }

            // 7. Lọc theo khoảng diện tích (minArea - maxArea)
            if (request.getMinArea() != null && request.getMinArea() > 0) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("area"), request.getMinArea()));
            }
            if (request.getMaxArea() != null && request.getMaxArea() > 0) {
                predicates.add(cb.lessThanOrEqualTo(root.get("area"), request.getMaxArea()));
            }

            // 8. Lọc theo danh sách tiện ích bắt buộc (AND logic: phòng phải có TẤT CẢ tiện ích đã chọn)
            if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
                for (Long amenityId : request.getAmenityIds()) {
                    Subquery<Long> subquery = query.subquery(Long.class);
                    var subRoot = subquery.from(Room.class);
                    Join<Room, Amenity> joinAmenity = subRoot.join("amenities");

                    subquery.select(subRoot.get("id"))
                            .where(
                                    cb.equal(subRoot.get("id"), root.get("id")),
                                    cb.equal(joinAmenity.get("id"), amenityId)
                            );

                    predicates.add(cb.exists(subquery));
                }
            }

            // 9. Lọc theo bán kính tọa độ vị trí (Bounding Box quanh tâm latitude, longitude)
            if (request.getLatitude() != null && request.getLongitude() != null
                    && request.getRadiusKm() != null && request.getRadiusKm() > 0) {
                double lat = request.getLatitude();
                double lon = request.getLongitude();
                double radius = request.getRadiusKm();

                double deltaLat = radius / 111.0;
                double deltaLon = radius / (111.0 * Math.cos(Math.toRadians(lat)));

                predicates.add(cb.between(root.get("latitude"), lat - deltaLat, lat + deltaLat));
                predicates.add(cb.between(root.get("longitude"), lon - deltaLon, lon + deltaLon));
            }

            if (query != null) {
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
