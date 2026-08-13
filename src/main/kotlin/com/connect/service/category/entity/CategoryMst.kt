package com.connect.service.category.entity

import com.connect.service.comment.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 관리자가 관리하는 게시판 카테고리 목록.
 *
 * board_mst.category 는 지금처럼 문자열(카테고리 이름)을 그대로 저장하고 FK 를 걸지 않는다.
 * 따라서 이름을 변경할 때는 기존 게시글의 category 문자열도 함께 갱신해야 한다
 * (CategoryService.update 참고).
 *
 * 테이블명이 board_category_mst 인 이유: 같은 스키마에 이 앱과 무관한 category_mst
 * (id int / is_visible / rank) 테이블이 이미 존재해서 이름을 분리했다.
 */
@Entity
@Table(name = "board_category_mst")
data class CategoryMst(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 100)
    var name: String,

    // 앱의 카테고리 필터에 노출되는 순서 (작을수록 먼저)
    @Column(nullable = false)
    var sortOrder: Int = 0,

    // false 면 앱 목록에서 숨긴다. 이미 이 카테고리로 작성된 게시글은 그대로 유지된다.
    @Column(nullable = false)
    var isActive: Boolean = true
) : BaseEntity()
