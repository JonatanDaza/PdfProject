package com.example.PruebaPDFBox.record;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaginationRequest {
    private Integer page;
    private Integer size;
}
