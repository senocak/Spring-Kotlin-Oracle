package com.github.senocak.domain.dto

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page

@JsonPropertyOrder("page", "pages", "total", "sort", "sortBy", "items")
open class PaginationResponse<T, P>(
    page: Page<T>,
    items: List<P>,

    @Schema(example = "id", description = "Sort by", required = true, name = "sortBy", type = "String")
    var sortBy: String? = null,

    @Schema(example = "asc", description = "Sort", required = true, name = "sort", type = "String")
    var sort: String? = null
) : BaseDto() {
    @Schema(example = "1", description = "Current page", required = true, name = "page", type = "String")
    var page: Int = page.number + 1

    @Schema(example = "3", description = "Total pages", required = true, name = "pages", type = "String")
    var pages: Int = page.totalPages

    @Schema(example = "10", description = "Total elements", required = true, name = "total", type = "String")
    var total: Long = page.totalElements

    @ArraySchema(schema = Schema(description = "items", required = true, type = "ListDto"))
    var items: List<P>? = items

    override fun toString(): String = "PaginationResponse(page: $page, pages: $pages, total: $total, items: $items)"
}