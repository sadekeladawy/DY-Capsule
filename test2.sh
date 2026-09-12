sed -i '/val animatedWidth/,/}/d' app/src/main/java/com/example/CapsuleUI.kt
sed -i '/val animatedHeight/,/}/d' app/src/main/java/com/example/CapsuleUI.kt
sed -i '/DisposableEffect(Unit) {/,/onDispose { }/d' app/src/main/java/com/example/CapsuleUI.kt
sed -i '/}/d' app/src/main/java/com/example/CapsuleUI.kt # wait, no this is dangerous
