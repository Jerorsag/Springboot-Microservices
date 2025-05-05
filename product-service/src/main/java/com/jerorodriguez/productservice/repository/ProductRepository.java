package com.jerorodriguez.productservice.repository;

import com.jerorodriguez.productservice.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<Product, String> {
}
