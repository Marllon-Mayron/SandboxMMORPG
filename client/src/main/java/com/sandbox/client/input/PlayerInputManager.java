package com.sandbox.client.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;

public class PlayerInputManager implements InputProcessor {

    private Vector2 movementDirection;
    private boolean isSprinting;
    private boolean wantsDash;
    private boolean isDashing;
    private boolean inputBlocked;

    // Configuração de teclas
    private int upKey = Input.Keys.W;
    private int downKey = Input.Keys.S;
    private int leftKey = Input.Keys.A;
    private int rightKey = Input.Keys.D;
    private int sprintKey = Input.Keys.SHIFT_LEFT;
    private int dashKey = Input.Keys.SPACE;

    // Estados
    private float dashCooldownTimer;
    private boolean dashJustExecuted;

    // Callbacks
    private Runnable onDashCallback;
    private Runnable onSprintStartCallback;
    private Runnable onSprintEndCallback;

    public PlayerInputManager() {
        this.movementDirection = new Vector2(0, 0);
        this.isSprinting = false;
        this.wantsDash = false;
        this.isDashing = false;
        this.inputBlocked = false;
        this.dashCooldownTimer = 0;
        this.dashJustExecuted = false;
    }

    public void update(float delta) {
        if (inputBlocked) {
            movementDirection.set(0, 0);
            isSprinting = false;
            return;
        }

        if (dashCooldownTimer > 0) {
            dashCooldownTimer -= delta;
        }

        dashJustExecuted = false;

        // Processar movimento
        float moveX = 0, moveY = 0;

        if (Gdx.input.isKeyPressed(upKey)) moveY += 1;
        if (Gdx.input.isKeyPressed(downKey)) moveY -= 1;
        if (Gdx.input.isKeyPressed(leftKey)) moveX -= 1;
        if (Gdx.input.isKeyPressed(rightKey)) moveX += 1;

        if (moveX != 0 || moveY != 0) {
            float length = (float) Math.sqrt(moveX * moveX + moveY * moveY);
            moveX /= length;
            moveY /= length;
        }

        movementDirection.set(moveX, moveY);

        // Sprint
        boolean wantsSprint = Gdx.input.isKeyPressed(sprintKey) && (moveX != 0 || moveY != 0);

        if (wantsSprint && !isSprinting && onSprintStartCallback != null) {
            onSprintStartCallback.run();
        } else if (!wantsSprint && isSprinting && onSprintEndCallback != null) {
            onSprintEndCallback.run();
        }

        isSprinting = wantsSprint;

        // Dash
        if (wantsDash && dashCooldownTimer <= 0 && (moveX != 0 || moveY != 0)) {
            isDashing = true;
            if (onDashCallback != null) {
                onDashCallback.run();
            }
            dashJustExecuted = true;
            wantsDash = false;
        }

        if (!wantsDash && isDashing) {
            isDashing = false;
        }
    }

    public void setDashCooldown(float cooldownSeconds) {
        this.dashCooldownTimer = cooldownSeconds;
    }

    public Vector2 getMovementDirection() { return movementDirection; }
    public Vector2 getDashDirection() {
        if (movementDirection.x != 0 || movementDirection.y != 0) {
            return new Vector2(movementDirection);
        }
        return new Vector2(0, 1);
    }

    public boolean isMoving() { return movementDirection.x != 0 || movementDirection.y != 0; }
    public boolean isSprinting() { return isSprinting && isMoving(); }
    public boolean isDashing() { return isDashing; }
    public boolean isDashJustExecuted() { return dashJustExecuted; }

    public void setInputBlocked(boolean blocked) {
        this.inputBlocked = blocked;
        if (blocked) {
            movementDirection.set(0, 0);
            isSprinting = false;
            wantsDash = false;
        }
    }

    public void setOnDash(Runnable callback) { this.onDashCallback = callback; }
    public void setOnSprintStart(Runnable callback) { this.onSprintStartCallback = callback; }
    public void setOnSprintEnd(Runnable callback) { this.onSprintEndCallback = callback; }

    @Override
    public boolean keyDown(int keycode) {
        if (inputBlocked) return false;
        if (keycode == dashKey && dashCooldownTimer <= 0) {
            wantsDash = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (keycode == dashKey) {
            wantsDash = false;
            return true;
        }
        return false;
    }

    @Override public boolean keyTyped(char character) { return false; }
    @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean scrolled(float amountX, float amountY) { return false; }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
}