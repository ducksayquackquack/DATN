package org.example.datnnhom03.dto.home;

import java.util.List;

public class HomeResponseDTO {

    private List<HomeProductDTO> products;
    private List<DanhMucDTO> categories;

    public HomeResponseDTO(List<HomeProductDTO> products, List<DanhMucDTO> categories) {
        this.products = products;
        this.categories = categories;
    }

    public List<HomeProductDTO> getProducts() {
        return products;
    }

    public void setProducts(List<HomeProductDTO> products) {
        this.products = products;
    }

    public List<DanhMucDTO> getCategories() {
        return categories;
    }

    public void setCategories(List<DanhMucDTO> categories) {
        this.categories = categories;
    }
}