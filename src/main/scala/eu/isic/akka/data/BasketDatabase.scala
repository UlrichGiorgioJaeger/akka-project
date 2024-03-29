package eu.isic.akka.data

import akka.actor.FSM
import eu.isic.akka.data.BasketDatabase._
import eu.isic.akka.restserver.{DeliveryAdress, Product, User}


object BasketDatabase {


  sealed trait BasketData {}

  case class BasketContainer(products: List[Product]) extends BasketData

  case class PaidContainer(paid: startPaymentContainer) extends BasketData

  sealed trait BasketState {}

  sealed trait BasketCommand {}

  case class AddToBasket(product: Product) extends BasketCommand

  case object GetBasketInformation extends BasketCommand

  case object StartPayment extends BasketCommand

  case object PaymentDone extends BasketCommand

  case class startPaymentContainer(orderId: Int, productsInBasket: List[Product], deliveryAdress: DeliveryAdress) extends BasketData

  case class PaymentInProgressContainer(id: String, number: Int, products: List[Product]) extends BasketState

  case object Paid extends BasketState

  case object PaymentInProgress extends BasketState

  case object Unpaid extends BasketState

  sealed trait BasketResponse

  case object Ok1 extends BasketResponse

}


class BasketDatabase extends FSM[BasketState, BasketData] {
  private var orderId = 100

  when(Unpaid) {
    case Event(AddToBasket(product), container: BasketContainer) =>
      stay() using container.copy(product :: container.products)
    case Event(StartPayment, _) =>
      goto(PaymentInProgress)
  }
  when(PaymentInProgress) {
    case Event(PaymentInProgressContainer(user, number, products), _) =>
      val customer = User.USER_LIST.find(_.id == user)
      if (customer.isDefined) {
        customer.foreach { cust =>
          cust.adresses.foreach { adre =>
            if (adre.id == number) {
              this.sender() ! startPaymentContainer(orderId, products, adre)

            } else {

            }

          }
        }
        orderId += 1
      } else this.sender() ! " Could not find User"
      stay()

    case Event(GetBasketInformation, container: BasketContainer) =>
      this.sender() ! BasketContainer(products = container.products)
      stay()

    case Event(msg, _) =>
      println(s"Cannot handle $msg in ${this.stateName}")
      stay()
  }

  when(Paid) {
    case _ => stay()
  }
  whenUnhandled {
    case Event(GetBasketInformation, container: BasketContainer) =>
      this.sender() ! BasketContainer(products = container.products)
      stay()

    case Event(msg, _) =>
      println(s"Cannot handle $msg in ${this.stateName}")
      stay()
  }
  onTransition {
    case x -> y =>
      println(s"Going from $x to $y")
  }
  startWith(Unpaid, BasketContainer(products = List.empty))

}
