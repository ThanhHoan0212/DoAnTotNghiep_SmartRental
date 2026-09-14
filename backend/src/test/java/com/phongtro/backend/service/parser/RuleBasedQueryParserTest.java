package com.phongtro.backend.service.parser;

import com.phongtro.backend.dto.response.ParsedQueryResponse;
import com.phongtro.backend.entity.Amenity;
import com.phongtro.backend.repository.AmenityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleBasedQueryParserTest {

    @Mock
    private AmenityRepository amenityRepository;

    @InjectMocks
    private RuleBasedQueryParser parser;

    @BeforeEach
    void setUp() {
        Amenity a1 = Amenity.builder().id(1L).name("Điều hòa nhiệt độ").build();
        Amenity a2 = Amenity.builder().id(2L).name("Bình nóng lạnh").build();
        Amenity a3 = Amenity.builder().id(3L).name("Máy giặt chung").build();
        Amenity a4 = Amenity.builder().id(4L).name("Vệ sinh khép kín").build();
        Amenity a5 = Amenity.builder().id(5L).name("Ban công thoáng mát").build();

        org.mockito.Mockito.lenient().when(amenityRepository.findAll()).thenReturn(List.of(a1, a2, a3, a4, a5));
    }

    @Test
    @DisplayName("Parse câu truy vấn có quận, giá trần, và nhiều tiện ích")
    void parse_QueryWithDistrictPriceAmenities() {
        String query = "tìm phòng trọ khép kín ở Bình Thạnh dưới 4 triệu có điều hòa và máy giặt";
        ParsedQueryResponse response = parser.parse(query);

        assertNotNull(response);
        assertEquals("Bình Thạnh", response.getDistrict());
        assertEquals(4000000.0, response.getMaxPrice());
        assertNull(response.getMinPrice());
        assertTrue(response.getDetectedAmenities().contains("Vệ sinh khép kín"));
        assertTrue(response.getDetectedAmenities().contains("Điều hòa nhiệt độ"));
        assertTrue(response.getDetectedAmenities().contains("Máy giặt chung"));
        assertEquals(3, response.getAmenityIds().size());
    }

    @Test
    @DisplayName("Parse câu truy vấn khoảng giá 'từ X đến Y tr' và diện tích")
    void parse_QueryWithPriceRangeAndArea() {
        String query = "cần thuê phòng từ 2.5 đến 3.5 tr ở Quận 1 trên 25m2";
        ParsedQueryResponse response = parser.parse(query);

        assertNotNull(response);
        assertEquals("Quận 1", response.getDistrict());
        assertEquals(2500000.0, response.getMinPrice());
        assertEquals(3500000.0, response.getMaxPrice());
        assertEquals(25.0, response.getMinArea());
    }

    @Test
    @DisplayName("Parse câu truy vấn dạng giá 'tầm X triệu', từ khóa studio và alias quận q3")
    void parse_QueryWithApproxPriceAndKeyword() {
        String query = "phòng trọ studio q3 tầm 5 triệu có ban công";
        ParsedQueryResponse response = parser.parse(query);

        assertNotNull(response);
        assertEquals("Quận 3", response.getDistrict());
        assertEquals("studio", response.getKeyword());
        assertEquals(4500000.0, response.getMinPrice());
        assertEquals(5500000.0, response.getMaxPrice());
        assertTrue(response.getDetectedAmenities().contains("Ban công thoáng mát"));
    }
}
