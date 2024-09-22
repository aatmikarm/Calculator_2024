import android.animation.ObjectAnimator
import android.view.View

// Function to start glow animation and return the animator instance
fun View.startGlowAnimation(): ObjectAnimator {
    val animator = ObjectAnimator.ofFloat(this, "alpha", 0.5f, 1f)
    animator.duration = 400
    animator.repeatMode = ObjectAnimator.REVERSE
//    animator.repeatCount = ObjectAnimator.INFINITE
    animator.repeatCount = ObjectAnimator.INFINITE
    animator.start()
    return animator // Return the ObjectAnimator instance to control it later
}

// Function to stop the glow animation (requires animator reference)
fun View.stopGlowAnimation(animator: ObjectAnimator?) {
    animator?.cancel()    // Cancel the ObjectAnimator to stop the animation
    this.alpha = 1f       // Reset the alpha value to full opacity
}
