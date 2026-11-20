package com.example.maidbooking.service.slot;

import com.example.maidbooking.enums.BookingType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class SlotGeneratorRegistry {

    private final Map<BookingType, SlotGenerator> generators = new EnumMap<>(BookingType.class);

    public SlotGeneratorRegistry(List<SlotGenerator> generators) {
        generators.forEach(generator -> this.generators.put(generator.type(), generator));
    }

    public SlotGenerator get(BookingType type) {
        SlotGenerator generator = generators.get(type);
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported booking type: " + type);
        }
        return generator;
    }
}
