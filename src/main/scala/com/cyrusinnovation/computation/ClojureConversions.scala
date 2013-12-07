package com.cyrusinnovation.computation

import clojure.lang._
import scala.collection.JavaConversions._
import java.util

object ClojureConversions {
  implicit def toClojureMap(map: Map[Any, Any]) : IPersistentMap = {
    PersistentHashMap.create(mapAsJavaMap(map))
  }

  implicit def toClojureList(list: List[Any]) : IPersistentList = {
    PersistentList.create(seqAsJavaList(list))
  }

  implicit def toClojureKeyword(KeywordString: String) : Keyword = {
    Keyword.intern(KeywordString)
  }

  implicit def fromClojureMap(map: IPersistentMap) : Map[Any, Any] = {
    map.asInstanceOf[util.Map[Any, Any]].toMap
  }

  def entityValueKeyFor(entityKeyword: Keyword, valueKeyword: Keyword) : IPersistentList = {
    List(entityKeyword, valueKeyword)
  }
}

