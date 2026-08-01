/* quiz.js — reusable quiz widget for lessons.
 *
 * Usage:
 *   <div class="quiz" data-right="Correct!" data-wrong="Not quite — try again.">
 *     <p>Question text?</p>
 *     <button data-value="A">Option A</button>
 *     <button data-value="B">Option B</button>
 *     <button data-value="C">Option C</button>
 *     <p class="feedback" aria-live="polite"></p>
 *   </div>
 *   <script>checkAnswer(this, "A")</script> on each button's onclick.
 */
(function () {
  "use strict";
  window.checkAnswer = function (btn, correctValue) {
    var card = btn.closest(".quiz");
    if (!card) return;
    var feedback = card.querySelector(".feedback");
    var wrong = btn.getAttribute("data-value") !== String(correctValue);

    if (wrong) {
      btn.classList.add("quiz-wrong");
      feedback.classList.add("quiz-feedback-wrong");
      feedback.textContent =
        card.getAttribute("data-wrong") || "Not quite — try again.";
      btn.disabled = true;
    } else {
      var buttons = card.querySelectorAll("button");
      for (var i = 0; i < buttons.length; i++) {
        buttons[i].classList.add("quiz-correct");
        buttons[i].disabled = true;
      }
      feedback.classList.remove("quiz-feedback-wrong");
      feedback.textContent = card.getAttribute("data-right") || "Correct!";
    }
    feedback.classList.add("quiz-feedback-visible");
  };
})();
