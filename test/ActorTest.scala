import akka.actor.{ActorSystem, Actor, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{Suite, BeforeAndAfterAll}
import scala.concurrent.duration._
import akka.testkit._

abstract class ActorTest(_system: ActorSystem) extends TestKit(_system) with ImplicitSender with Suite with BeforeAndAfterAll {

  override def afterAll = {
    TestKit.shutdownActorSystem(system)
  }

  object Mock {
    def dummy = Props(new Actor { def receive = { case _: Any => } })
    def resendToTestActor = Props(new Actor { def receive = { case msg => testActor ! msg } })
    def sendToTestActorWhenCreated(msg: Any) = Props(new Actor { testActor ! msg; def receive = { case _: Any => } })
    def sendToParentWhenCreated(msg: Any) = Props(new Actor { context.parent ! msg; def receive = { case _: Any => } })
  }

  def commonTimeout = (1 second).dilated

}
