package com.visal.hybridIDGen.controller;

import com.visal.hybridIDGen.entity.Item;
import com.visal.hybridIDGen.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @PostMapping("/auto")
    public ResponseEntity<Item> createWithAutoId(@RequestBody Map<String, String> payload) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itemService.createWithAutoId(payload.get("name")));
    }

    @PostMapping("/manual")
    public ResponseEntity<Item> createWithManualId(@RequestBody Map<String, Object> payload) {
        Object rawId = payload.get("id");
        Integer id = null;
        if (rawId != null) {
            try {
                id = Integer.parseInt(rawId.toString());
            } catch (NumberFormatException e) {
                throw new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "'id' must be an integer");
            }
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(itemService.createWithManualId(id, (String) payload.get("name")));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable Integer id,
                                           @RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(itemService.updateItem(id, payload.get("name")));
    }
}
