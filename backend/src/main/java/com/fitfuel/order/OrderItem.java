package com.fitfuel.order;

import com.fitfuel.menu.FoodItem;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    // Nullable: admins can delete a menu item at any time (even with order history).
    // The order still needs to render correctly afterward, so the name is snapshotted
    // below at order-creation time rather than always read live off this reference.
    @ManyToOne
    @JoinColumn(name = "food_item_id")
    private FoodItem foodItem;

    // Snapshot of the food item's name at the moment the order was placed, so order
    // history keeps reading correctly even after the menu item itself is deleted.
    @Column(name = "food_item_name", nullable = false)
    private String foodItemName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal price;

    public Long getId() {
        return id;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public void setOrder(CustomerOrder order) {
        this.order = order;
    }

    public FoodItem getFoodItem() {
        return foodItem;
    }

    public void setFoodItem(FoodItem foodItem) {
        this.foodItem = foodItem;
    }

    public String getFoodItemName() {
        return foodItemName;
    }

    public void setFoodItemName(String foodItemName) {
        this.foodItemName = foodItemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }
}
