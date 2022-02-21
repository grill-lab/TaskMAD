import { InteractionType } from "../generated/client_pb"
import { IMessage, Message } from "./MessageModel"

export interface IDialogue {
  messages: IMessage[]
}

export class Dialogue implements IDialogue {
  constructor(model: IDialogue) {
    Object.assign(this, model)
  }

  // noinspection JSUnusedGlobalSymbols
  public messages!: IMessage[]

  public hasMessage = (message: Message) => {
    return undefined !== this.messages.find(
      (existingMessage) => (existingMessage.id === message.id))
  }

  public appending = (message: Message, durationBetweenDatesInSec: number) => {
    // if the message with this ID exists, do not add it
    if (this.hasMessage(message)) {
      return this
    }

    const messages: Message[] = [message]
    const time = message.time
    if (this.messages.length !== 0 && durationBetweenDatesInSec !== 0) {
      const lastMessageTime = this.messages[this.messages.length - 1].time
      if (lastMessageTime.getTime()
        < (time.getTime() - durationBetweenDatesInSec * 1000)) {

        // We show the time message only if the message sent is text (and not a status message)
        if (message.messageType === InteractionType.TEXT) {
          messages.unshift(new Message(time))
        }

      }
    }
    return new Dialogue({
      messages: this.messages.concat(messages),
    })
  }
}

export const US = "us"
export const THEM = "them"

// noinspection SpellCheckingInspection
export const sampleDialogue = () => new Dialogue({
  messages: [
    new Message({ text: "Begin" }),
    new Message({ userID: "them", text: "Hello" }),
    new Message({ userID: US, text: "How are you" }),
    new Message({ userID: THEM, text: "I'm well" }),
    new Message({ text: "More chat" }),
    new Message({
      userID: US,
      text: "Your bones don't break, mine do. That's clear. Your cells "
        + "react to bacteria and viruses differently than mine. "
        + "You don't get sick, I do. That's also clear. But for some "
        + "reason, you and I react the exact same way to water. We "
        + "swallow it too fast, we choke. We get some in our lungs, "
        + "we drown. However unreal it may seem, we are connected, "
        + "you and I. We're on the same curve, just on opposite ends."
    }),
    new Message({
      userID: THEM,
      text: "You think water moves fast? You should see ice. It moves like "
        + "it has a mind. Like it knows it killed the world once and "
        + "got a taste for murder. After the avalanche, it took us a "
        + "week to climb out. Now, I don't know exactly when we turned "
        + "on each other, but I know that seven of us survived the "
        + "slide... and only five made it out. Now we took an oath, "
        + "that I'm breaking now. We said we'd say it was the snow "
        + "that killed the other two, but it wasn't. Nature is lethal "
        + "but it doesn't hold a candle to man.\n"
    }),
    new Message({
      userID: US,
      text: "Look, just because I don't be givin' no man a foot massage "
        + "don't make it right for Marsellus to throw Antwone into a "
        + "glass motherfuckin' house, fuckin' up the way the nigger "
        + "talks. Motherfucker do that shit to me, he better paralyze "
        + "my ass, 'cause I'll kill the motherfucker, know what I'm "
        + "sayin'?\n"
    }),
    new Message({
      userID: US,
      text: "Your bones don't break, mine do. That's clear. Your cells "
        + "react to bacteria and viruses differently than mine. "
        + "You don't get sick, I do. That's also clear. But for some "
        + "reason, you and I react the exact same way to water. We "
        + "swallow it too fast, we choke. We get some in our lungs, "
        + "we drown. However unreal it may seem, we are connected, "
        + "you and I. We're on the same curve, just on opposite ends."
    }),
    new Message({
      userID: THEM,
      text: "You think water moves fast? You should see ice. It moves like "
        + "it has a mind. Like it knows it killed the world once and "
        + "got a taste for murder. After the avalanche, it took us a "
        + "week to climb out. Now, I don't know exactly when we turned "
        + "on each other, but I know that seven of us survived the "
        + "slide... and only five made it out. Now we took an oath, "
        + "that I'm breaking now. We said we'd say it was the snow "
        + "that killed the other two, but it wasn't. Nature is lethal "
        + "but it doesn't hold a candle to man.\n"
    }),
    new Message({
      userID: US,
      text: "Look, just because I don't be givin' no man a foot massage "
        + "don't make it right for Marsellus to throw Antwone into a "
        + "glass motherfuckin' house, fuckin' up the way the nigger "
        + "talks. Motherfucker do that shit to me, he better paralyze "
        + "my ass, 'cause I'll kill the motherfucker, know what I'm "
        + "sayin'?\n"
    }),
  ]
})
