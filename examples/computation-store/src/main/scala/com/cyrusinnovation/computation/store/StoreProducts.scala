package com.cyrusinnovation.computation.store

import scala.collection.mutable.ListBuffer
import scala.collection.mutable

case class StoreProduct(id: Long, name: String, cost: BigDecimal)

object StoreProducts {
  private var products = mutable.Set(
    StoreProduct(1, "P=NP", BigDecimal("12.23")),
    StoreProduct(2, "Fermats Last Theorem", BigDecimal("5.23")),
    StoreProduct(3, "Fast Integer factorization", BigDecimal("35.99")),
    StoreProduct(4, "Riemann hypothesis", BigDecimal("99.23"))
  )

  def all: List[StoreProduct] = {
    products.toList
  }

  def byId(id: Long): Option[StoreProduct] = {
    products.find(_.id == id)
  }

  def create(product: StoreProduct): StoreProduct = {
    val productWithUpdatedId = product.copy(id = nextId)
    products.add(productWithUpdatedId)
    productWithUpdatedId
  }

  def update(updatedProduct: StoreProduct): StoreProduct = {
    products.remove(products.find(_.id == updatedProduct.id).get)
    products.add(updatedProduct)
    updatedProduct
  }

  def delete(productId: Long) {
    products.remove(products.find(_.id == productId).get)
  }

  private def nextId = products.maxBy(_.id).id + 1
}
