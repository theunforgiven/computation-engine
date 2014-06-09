package com.cyrusinnovation.computation.store.store

import org.scalatra._
import scala.util.Try
import com.cyrusinnovation.computation.store._
import scala.Some


class ProductsServlet extends ComputationStoreServlet {
  get("/") {
    StoreProducts.all
  }

  post("/") {
    Some(parsedBody.extract[StoreProduct])
      .fold(BadRequest("Could not create product from argument"))(x => Ok(StoreProducts.create(x)))
  }

  get("/:productId") {
    Try(params("productId").toInt)
      .map(StoreProducts.byId(_).get)
      .map(Ok(_))
      .getOrElse(NotFound("Sorry, the product could not be found"))
  }

  put("/:productId") {
    Some(parsedBody.extract[StoreProduct])
      .fold(BadRequest("Could not create product from argument"))(x => Ok(StoreProducts.update(x)))
  }

  delete("/:productId") {
    def doDelete(id: Int) = { StoreProducts.delete(id); JsonNoContent }
    Try(params("productId").toInt).map(x => doDelete(x))
      .getOrElse(NotFound("Sorry, the product could not be found"))
  }
}

