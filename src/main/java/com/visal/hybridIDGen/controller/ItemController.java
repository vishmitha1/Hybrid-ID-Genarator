package com.visal.hybridIDGen.controller;

import com.visal.hybridIDGen.entity.Item;
import com.visal.hybridIDGen.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemRepository itemRepository;

    /**
     * POST /items/auto
     * id is null → generatedOnExecution(entity,session) returns true → PostgreSQL SERIAL assigns it.
     */
    @PostMapping("/auto")
    public ResponseEntity<Item> createWithAutoId(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'name' is required");
        }

        Item item = Item.builder().name(name.trim()).build();
        Item saved = itemRepository.save(item);
        log.info("[auto] saved item: {}", saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * POST /items/manual
     * id is set → generatedOnExecution(entity,session) returns false → generate() returns it as-is.
     */
    @PostMapping("/manual")
    public ResponseEntity<Item> createWithManualId(@RequestBody Map<String, Object> payload) {
        Object rawId = payload.get("id");
        String name  = (String) payload.get("name");

        if (rawId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'id' is required");
        }
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'name' is required");
        }

        Integer id;
        try {
            id = Integer.parseInt(rawId.toString());
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'id' must be an integer");
        }

        if (itemRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Item with id " + id + " already exists");
        }

        Item item = Item.builder().id(id).name(name.trim()).build();
        Item saved = itemRepository.save(item);
        log.info("[manual] saved item: {}", saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }
}
