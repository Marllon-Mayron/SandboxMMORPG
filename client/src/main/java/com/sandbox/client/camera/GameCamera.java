package com.sandbox.client.camera;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

public class GameCamera {

    private final OrthographicCamera camera;
    private final Vector2 targetPosition;
    private final Vector2 currentPosition;
    private final Vector2 shakeOffset;

    private float followSpeed;
    private float shakeIntensity;
    private float shakeDuration;
    private float shakeTime;

    private float minX;
    private float minY;
    private float maxX;
    private float maxY;
    private boolean boundsEnabled;

    private Vector2 deadZone;
    private boolean deadZoneEnabled;

    /**
     * Construtor padrao da camera.
     *
     * @param viewportWidth Largura da viewport
     * @param viewportHeight Altura da viewport
     */
    public GameCamera(float viewportWidth, float viewportHeight) {
        this.camera = new OrthographicCamera();
        this.camera.setToOrtho(false, viewportWidth, viewportHeight);

        this.targetPosition = new Vector2();
        this.currentPosition = new Vector2();
        this.shakeOffset = new Vector2();

        // Inicializa com valores padrao (0,0) mas sera atualizado pelo reset quando o player chegar
        this.followSpeed = 8.0f;
        this.boundsEnabled = false;
        this.deadZoneEnabled = false;
        this.deadZone = new Vector2(50, 50);
    }

    /**
     * Define a posicao alvo da camera (posicao do jogador).
     *
     * @param x Posicao X alvo
     * @param y Posicao Y alvo
     */
    public void setTarget(float x, float y) {
        targetPosition.set(x, y);
    }

    /**
     * Define a posicao alvo da camera.
     *
     * @param target Vetor com a posicao alvo
     */
    public void setTarget(Vector2 target) {
        this.targetPosition.set(target);
    }

    /**
     * Atualiza a posicao da camera com suavidade.
     * Deve ser chamado uma vez por frame.
     *
     * @param delta Tempo decorrido desde o ultimo frame (em segundos)
     */
    public void update(float delta) {
        float targetX = targetPosition.x;
        float targetY = targetPosition.y;

        // Aplica dead zone se estiver habilitada
        if (deadZoneEnabled) {
            float diffX = targetX - currentPosition.x;
            float diffY = targetY - currentPosition.y;

            if (Math.abs(diffX) > deadZone.x) {
                targetX = currentPosition.x + Math.signum(diffX) * deadZone.x;
            }
            if (Math.abs(diffY) > deadZone.y) {
                targetY = currentPosition.y + Math.signum(diffY) * deadZone.y;
            }
        }

        // Interpolacao linear suave (lerp)
        float alpha = MathUtils.clamp(followSpeed * delta, 0, 1);
        currentPosition.x = currentPosition.x + (targetX - currentPosition.x) * alpha;
        currentPosition.y = currentPosition.y + (targetY - currentPosition.y) * alpha;

        // Aplica shake se estiver ativo
        applyShake(delta);

        // Define a posicao final da camera
        float finalX = currentPosition.x + shakeOffset.x;
        float finalY = currentPosition.y + shakeOffset.y;

        // Aplica limites se estiverem habilitados
        if (boundsEnabled) {
            finalX = MathUtils.clamp(finalX, minX, maxX);
            finalY = MathUtils.clamp(finalY, minY, maxY);
        }

        camera.position.set(finalX, finalY, 0);
        camera.update();
    }

    /**
     * Atualiza a matriz de projecao e visao da camera.
     * Geralmente chamado apos resize da tela.
     */
    public void updateViewport() {
        camera.update();
    }

    /**
     * Aplica o efeito de tremor (shake) na camera.
     *
     * @param delta Tempo decorrido desde o ultimo frame
     */
    private void applyShake(float delta) {
        if (shakeTime > 0) {
            shakeTime -= delta;

            if (shakeTime <= 0) {
                shakeOffset.setZero();
                shakeIntensity = 0;
            } else {
                float intensity = shakeIntensity * (shakeTime / shakeDuration);
                shakeOffset.x = (MathUtils.random(-1f, 1f)) * intensity;
                shakeOffset.y = (MathUtils.random(-1f, 1f)) * intensity;
            }
        }
    }

    /**
     * Inicia um efeito de tremor na camera.
     *
     * @param intensity Intensidade do tremor (em pixels)
     * @param duration Duracao do tremor (em segundos)
     */
    public void shake(float intensity, float duration) {
        this.shakeIntensity = intensity;
        this.shakeDuration = duration;
        this.shakeTime = duration;
    }

    /**
     * Define a velocidade de seguimento da camera.
     * Valores maiores = seguimento mais rapido, menor suavidade.
     *
     * @param speed Velocidade de seguimento (recomendado: 5-15)
     */
    public void setFollowSpeed(float speed) {
        this.followSpeed = Math.max(1, speed);
    }

    /**
     * Obtem a velocidade de seguimento atual.
     *
     * @return Velocidade de seguimento
     */
    public float getFollowSpeed() {
        return followSpeed;
    }

    /**
     * Configura os limites de movimento da camera.
     *
     * @param minX Limite minimo X
     * @param minY Limite minimo Y
     * @param maxX Limite maximo X
     * @param maxY Limite maximo Y
     */
    public void setBounds(float minX, float minY, float maxX, float maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.boundsEnabled = true;
    }

    /**
     * Desabilita os limites de movimento da camera.
     */
    public void disableBounds() {
        this.boundsEnabled = false;
    }

    /**
     * Verifica se os limites estao habilitados.
     *
     * @return true se os limites estiverem ativos
     */
    public boolean isBoundsEnabled() {
        return boundsEnabled;
    }

    /**
     * Configura a zona morta (dead zone) da camera.
     * A camera so se move quando o jogador sai da zona morta.
     *
     * @param width Largura da zona morta (em pixels)
     * @param height Altura da zona morta (em pixels)
     */
    public void setDeadZone(float width, float height) {
        this.deadZone.set(width, height);
        this.deadZoneEnabled = true;
    }

    /**
     * Desabilita a zona morta (dead zone).
     */
    public void disableDeadZone() {
        this.deadZoneEnabled = false;
    }

    /**
     * Redefine a posicao da camera instantaneamente para o alvo.
     * Util para teleportes ou inicio de jogo.
     */
    public void resetPosition() {
        if (targetPosition != null) {
            currentPosition.set(targetPosition);
            camera.position.set(targetPosition.x, targetPosition.y, 0);
            camera.update();
        }
    }

    /**
     * Centraliza a camera na posicao atual do alvo imediatamente.
     */
    public void snapToTarget() {
        currentPosition.set(targetPosition);
    }

    /**
     * Obtem a camera OrthographicCamera interna.
     *
     * @return Referencia para a camera LibGDX
     */
    public OrthographicCamera getCamera() {
        return camera;
    }

    /**
     * Converte coordenadas de tela para mundo.
     *
     * @param screenX Coordenada X da tela
     * @param screenY Coordenada Y da tela
     * @return Vetor com as coordenadas do mundo
     */
    public Vector3 unproject(float screenX, float screenY) {
        return camera.unproject(new Vector3(screenX, screenY, 0));
    }

    /**
     * Converte coordenadas de mundo para tela.
     *
     * @param worldX Coordenada X do mundo
     * @param worldY Coordenada Y do mundo
     * @return Vetor com as coordenadas da tela
     */
    public Vector3 project(float worldX, float worldY) {
        return camera.project(new Vector3(worldX, worldY, 0));
    }

    /**
     * Redimensiona a viewport da camera.
     *
     * @param width Nova largura
     * @param height Nova altura
     */
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    /**
     * Obtem a posicao atual da camera no mundo.
     *
     * @return Vetor com a posicao atual
     */
    public Vector2 getPosition() {
        return new Vector2(camera.position.x, camera.position.y);
    }

    /**
     * Verifica se um objeto está dentro da view da camera.
     * Útil para culling (não renderizar o que está fora da tela).
     *
     * @param x Posição X do objeto
     * @param y Posição Y do objeto
     * @param width Largura do objeto
     * @param height Altura do objeto
     * @return true se o objeto está visível na camera
     */
    public boolean isInView(float x, float y, float width, float height) {
        float viewLeft = camera.position.x - camera.viewportWidth / 2;
        float viewRight = camera.position.x + camera.viewportWidth / 2;
        float viewBottom = camera.position.y - camera.viewportHeight / 2;
        float viewTop = camera.position.y + camera.viewportHeight / 2;

        return (x + width > viewLeft && x - width < viewRight &&
                y + height > viewBottom && y - height < viewTop);
    }
}