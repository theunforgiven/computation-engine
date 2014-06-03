package com.cyrusinnovation.computation.store.store

import com.cyrusinnovation.computation.store.{StoreProduct, StoreProducts}
import org.scalatra.{NoContent, BadRequest, NotFound, Ok}
import scala.util.Try


class ProductsServlet extends ComputationStoreServlet {
  get("/") {
    StoreProducts.all
  }

  put("/") {
    Some(parsedBody.extract[StoreProduct])
      .fold(BadRequest("Could not create product from argument"))(x => Ok(StoreProducts.create(x)))
  }

  get("/:productId") {
    Try(params("productId").toInt)
      .map(StoreProducts.byId(_).get)
      .map(Ok(_))
      .getOrElse(NotFound("Sorry, the product could not be found"))
  }

  post("/:productId") {
    Some(parsedBody.extract[StoreProduct])
      .fold(BadRequest("Could not create product from argument"))(x => Ok(StoreProducts.update(x)))
  }

  delete("/:productId") {
    def doDelete(id: Int) = StoreProducts.delete(id); NoContent()
    Try(params("productId").toInt).map(x => doDelete(x))
                                  .getOrElse(NotFound("Sorry, the product could not be found"))
  }
}

