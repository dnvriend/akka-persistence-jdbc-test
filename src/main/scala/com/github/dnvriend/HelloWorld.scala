/*
 * Copyright 2017 Dennis Vriend
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.dnvriend

import java.util.UUID

import akka.actor.{ ActorLogging, ActorRef, ActorSystem, Props }
import akka.pattern.ask
import akka.persistence._
import akka.util.Timeout

import scala.compat.Platform
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

case class Person(name: String, age: Int)

case object GetState

case object SaveSnapshot

case object Terminate

case class CreatePerson(name: String, age: Int)

case class PersonCreated(name: String, age: Int, createdAt: Long)

class PersonActor(override val persistenceId: String)(implicit ec: ExecutionContext) extends PersistentActor with ActorLogging {
  println(s"Creating actor: $persistenceId")
  var state: Option[Person] = None
  var replyTo: Option[ActorRef] = None

  override def receiveRecover: Receive = {
    case offer @ SnapshotOffer(meta, snapshot: Person) =>
      println(s"Recover from snapshot: '$offer'")
      state = Option(snapshot)
    case event @ PersonCreated(name, age, createdAt) =>
      state = Option(Person(name, age))
      println(s"Recover from event: '$event' createdAt: '$createdAt'")
    case RecoveryCompleted =>
      println(s"RecoveryCompleted, state='$state'")
    case unknown => println(s"Dropping: $unknown")
  }

  override def receiveCommand: Receive = {
    case cmd @ CreatePerson(name, age) =>
      persist(PersonCreated(name, age, Platform.currentTime)) { event =>
        println(s"Handling command: '$cmd'")
        state = Option(Person(name, age))
        sender() ! event
      }
    case GetState =>
      println(s"Getting state: $state")
      sender() ! state
    case SaveSnapshot =>
      state.foreach { person =>
        println(s"Saving snapshot: $state")
        saveSnapshot(person)
        replyTo = Option(sender())
      }

    case SaveSnapshotSuccess(meta) =>
      println(s"Snapshot saved: $meta")
      replyTo.foreach(_ ! akka.actor.Status.Success(s"snapshot saved: $meta"))
      replyTo = None
    case SaveSnapshotFailure(meta, t) =>
      println(s"Snapshot NOT saved: $meta, $t")
      replyTo.foreach(_ ! akka.actor.Status.Failure(t))
      replyTo = None

    case Terminate =>
      println(s"Terminating actor: $self")
      context.stop(self)
      replyTo = Option(sender())
  }

  override def postStop(): Unit = {
    println(s"Terminated: $self")
    replyTo.foreach(_ ! akka.actor.Status.Success("terminated"))
  }
}

object HelloWorld extends App {
  implicit val system: ActorSystem = ActorSystem()
  implicit val timeout: Timeout = 1.second

  val personId: String = UUID.randomUUID().toString
  val ref: ActorRef = system.actorOf(Props(new PersonActor(personId)), personId)

  val f = (for {
    startState <- (ref ? GetState).mapTo[Option[Person]]
    _ = require(startState.isEmpty, "Start state should be empty")
    _ <- (ref ? CreatePerson("foo", 42)).mapTo[PersonCreated]
    _ <- ref ? SaveSnapshot
    _ <- ref ? Terminate
    ref2 = system.actorOf(Props(new PersonActor(personId)), personId)
    recoveredState <- (ref2 ? GetState).mapTo[Option[Person]]
    _ = require(recoveredState.nonEmpty, "Recovered should be nonEmpty")
    _ = require(recoveredState.contains(Person("foo", 42)), "RecoveredState contain a recovered Person")
    _ <- system.terminate
  } yield ()).recoverWith {
    case t: Throwable =>
      t.printStackTrace()
      system.terminate
  }
}