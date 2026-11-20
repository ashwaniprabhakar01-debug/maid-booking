package com.example.maidbooking.repository.inmemory;

import com.example.maidbooking.domain.Maid;
import com.example.maidbooking.repository.MaidRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryMaidRepository implements MaidRepository {

    private final Map<String, Maid> maids = new ConcurrentHashMap<>();

    @Override
    public Maid save(Maid maid) {
        maids.put(maid.getId(), maid);
        return maid;
    }

    @Override
    public Optional<Maid> findById(String id) {
        return Optional.ofNullable(maids.get(id));
    }

    @Override
    public List<Maid> findAll() {
        return new ArrayList<>(maids.values());
    }
}
