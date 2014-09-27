package core

package object utils {

  /**
   * Helper that allows to call private methods on Scala/Java objects.
   *
   * Usage: `someObject.exposeMethod('methodName)(arg1, arg2, arg3)`
   *
   * See: https://gist.github.com/EugenyLoy/5873642543f869c7e25f
   */
  implicit class ExposePrivateMethods(obj: AnyRef) {

    def exposeMethod(methodName: scala.Symbol)(_args: Any*): Any = {
      val args = _args.map(_.asInstanceOf[AnyRef])
      def _parents: Stream[Class[_]] = Stream(obj.getClass) #::: _parents.map(_.getSuperclass)
      val parents = _parents.takeWhile(_ != null).toList
      val methods = parents.flatMap(_.getDeclaredMethods)
      methods.find(_.getName == methodName.name) match {
        case None =>
          throw new IllegalArgumentException(s"Method: ${methodName.name} not found on object: $obj")
        case Some(method) =>
          method.setAccessible(true)
          method.invoke(obj, args : _*)
      }
    }

  }

}
