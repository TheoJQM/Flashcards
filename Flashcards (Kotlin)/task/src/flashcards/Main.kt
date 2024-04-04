package flashcards

import java.io.File

const val goodGuess = "Correct!"
const val wrongGuess = "Wrong. The right answer is \"%s\"."
const val definitionOfAnotherCard = "Wrong. The right answer is \"%s\", but your definition is correct for \"%s\"."

class FlashCards(args: Array<String>) {
    private val cards = mutableListOf<Card>()
    private val log = mutableListOf<String>()
    private var exit = true
    private var importFile = File("")
    private var exportFile = File("")

    init {
        if (args.size % 2 == 0) {
            val listFile = mutableListOf<Pair<String, String>>()
            for (i in args.indices step 2) listFile.add(Pair(args[i], args[i + 1]))

            for (file in listFile) {
                when (file.first) {
                    "-import" -> importFile = File(file.second)
                    "-export" -> exportFile = File(file.second)
                }
            }

            if (importFile.exists()) importCards(importFile)
        }
    }

    fun memorize() {
        while (exit) {
            val action = println("Input the action (add, remove, import, export, ask, exit, log, hardest card, reset stats):".saveToLog()).run{ readln().saveToLog()}
            when (action) {
                "add" -> addCard()
                "remove" -> removeCard()
                "import" -> import()
                "export" -> export()
                "ask" -> ask()
                "exit" -> exit = false
                "log" -> log()
                "hardest card" -> getHardestCard()
                "reset stats" -> resetStats()
            }
        }
        if (exportFile != File("")) {
            exportFile.writeText(cards.joinToString("\n"))
            println("${cards.size} cards have been saved.\n".saveToLog())
        }
        println("Bye bye!".saveToLog())
    }

    private fun addCard() {
        val term = println("The card:".saveToLog()).run { readln().saveToLog() }
        if (!isTermAvailable(term)) {
            println("The card \"$term\" already exists.\n".saveToLog())
            return
        }

        val definition = println("The definition of the card:".saveToLog()).run { readln().saveToLog() }
        if (!isDefinitionAvailable(definition)) {
            println("The definition \"$definition\" already exists.\n".saveToLog())
            return
        }

        cards.add(Card(term, definition))
        println("The pair (\"$term\":\"$definition\") has been added.\n".saveToLog())
    }

    private fun isTermAvailable(newTerm: String) = !cards.any { it.getTerm() == newTerm }

    private fun isDefinitionAvailable(newDefinition: String) = !cards.any { it.getDefinition() == newDefinition }

    private fun removeCard() {
        val cardToRemove = println("Which card?".saveToLog()).run { readln().saveToLog() }
        val card = cards.filter { it.getTerm() == cardToRemove }
        if (card.isNotEmpty()) {
            cards.removeIf { it.getTerm() == cardToRemove }
            println("The card has been removed.\n".saveToLog())
        } else {
            println("Can't remove \"$cardToRemove\": there is no such card.\n".saveToLog())
        }
    }

    private fun import() {
        val file = println("File name:".saveToLog()).run { File(readln().saveToLog()) }
        if (!file.exists()) println("File not found.".saveToLog()) else importCards(file)
    }

    private fun importCards(file: File) {
        val regex = Regex("""(\d+[A-Za-z]+\s?[A-Za-z]+:[A-Za-z]+)+""")
        val lines = file.readLines()

        for (line in lines) {
            val newCards = regex.find(line)
            newCards?.groups?.forEach {oneCard ->
                val card = oneCard!!.value.split(":")
                val newCard = Card(card[0].drop(1), card.last(), card[0].take(1).toInt())
                when {
                    cards.any { it.getTerm() == newCard.getTerm() } -> {
                        val oldCardIndex = cards.indexOfFirst { it.getTerm() == newCard.getTerm() }
                        cards[oldCardIndex] = newCard
                    }
                    else -> {
                        cards.add(newCard)
                    }
                }
            }
        }
        println("${lines.size} cards have been loaded.\n".saveToLog())
    }

    private fun export() {
        val file = println("File name:".saveToLog()).run { File(readln().saveToLog()) }
        file.writeText(cards.joinToString("\n"))
        println("${cards.size} cards have been saved.\n".saveToLog())
    }

    private fun ask() {
        val cardToLearn = println("How many times to ask?".saveToLog()).run { readln().saveToLog().toInt() }
        repeat(cardToLearn) {
            val randomCard = cards[it % cards.size]
            val userInput = println("Print the definition of \"${randomCard.getTerm()}\":".saveToLog()).run { readln().saveToLog() }
            if (randomCard.checkUserTry(userInput))  println(goodGuess.saveToLog()) else checkOtherDefinition(randomCard, userInput)
        }
        println()
    }

    private fun checkOtherDefinition(currentCard: Card, input: String) {
        val card = cards.filter { it.getDefinition() == input }
        if (card.isNotEmpty()) {
            println(definitionOfAnotherCard.format(currentCard.getDefinition(), card.first().getTerm()).saveToLog())
        } else {
            println(wrongGuess.format(currentCard.getDefinition()).saveToLog())
        }
        val indexCard = cards.indexOfFirst { it == currentCard }
        cards[indexCard].incMistakes()
    }

    private fun log() {
        val file = println("File name:".saveToLog()).run { File(readln().saveToLog()) }
        file.appendText(log.joinToString("\n"))
        println("The log has been saved.".saveToLog())
    }

    private fun getHardestCard() {
        var cardsSortedDESC = cards.apply { sortByDescending { it.getMistakes() } }
        cardsSortedDESC = cardsSortedDESC.filter { it.getMistakes() == cardsSortedDESC[0].getMistakes() }.toMutableList()
        when {
            cardsSortedDESC.isEmpty() || cardsSortedDESC.first().getMistakes() == 0 -> println("There are no cards with errors.".saveToLog())
            cardsSortedDESC.size == 1 -> println("The hardest card is \"${cardsSortedDESC.first().getTerm()}\". You have ${cardsSortedDESC.first().getMistakes()} errors answering it.".saveToLog())
            else -> println("The hardest cards are \"${cardsSortedDESC.joinToString("\", \"") { it.getTerm() }}\". You have ${cardsSortedDESC.first().getMistakes()} errors answering them.".saveToLog())
        }
    }

    private fun resetStats() {
        cards.forEach { it.resetMistakes() }
        println("Card statistics have been reset.".saveToLog())
    }

    private fun String.saveToLog(): String {
        log.add(this)
        return this
    }
}

class Card(private var term: String, private var definition: String, private var mistakes: Int = 0) {
    fun getTerm() = term
    fun getDefinition() = definition
    fun getMistakes() = mistakes

    fun checkUserTry(input: String) = input == definition

    fun incMistakes() {
        mistakes++
    }

    fun resetMistakes() {
        mistakes = 0
    }

    override fun toString() = "$mistakes$term:$definition"
}

fun main(args: Array<String>) {
    val flashCards = FlashCards(args)
    flashCards.memorize()
}