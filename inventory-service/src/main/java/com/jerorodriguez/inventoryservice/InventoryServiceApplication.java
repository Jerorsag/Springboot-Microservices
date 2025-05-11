package com.jerorodriguez.inventoryservice;

import com.jerorodriguez.inventoryservice.model.Inventory;
import com.jerorodriguez.inventoryservice.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }

    @Bean
    public CommandLineRunner loadData(InventoryRepository inventoryRepository) {
        return args -> {
            Inventory inventory = new Inventory();
            inventory.setSkuCode("iphone_16");
            inventory.setQuantity(100);

            Inventory inventory1 = new Inventory();
            inventory1.setSkuCode("iphone_16_red");
            inventory1.setQuantity(0);

            Inventory inventory2 = new Inventory();
            inventory2.setSkuCode("hp_victus");
            inventory2.setQuantity(25);

            Inventory inventory3 = new Inventory();
            inventory3.setSkuCode("tv_lg_55");
            inventory3.setQuantity(5);

            inventoryRepository.save(inventory);
            inventoryRepository.save(inventory1);
            inventoryRepository.save(inventory2);
            inventoryRepository.save(inventory3);
        };
    }

}
