package com.visal.hybridIDGen.service;

import com.visal.hybridIDGen.entity.Item;
import com.visal.hybridIDGen.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public Item createWithAutoId(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'name' is required");
        }

        Item item = Item.builder().name(name.trim()).build();
        Item saved = itemRepository.save(item);
        log.info("[auto] saved item: {}", saved);
        return saved;
    }

    public Item createWithManualId(Integer id, String name) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'id' is required");
        }
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'name' is required");
        }
        if (itemRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Item with id " + id + " already exists");
        }

        Item item = Item.builder().id(id).name(name.trim()).build();
        Item saved = itemRepository.save(item);
        log.info("[manual] saved item: {}", saved);
        return saved;
    }

    public Item updateItem(Integer id, String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'name' is required");
        }

        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Item with id " + id + " not found"));

        item.setName(name.trim());
        Item updated = itemRepository.save(item);
        log.info("[update] updated item: {}", updated);
        return updated;
    }
}
