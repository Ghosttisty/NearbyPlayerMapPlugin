//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package NPMP.ghost.MapRenderer;

import NPMP.ghost.NearbyPlayersMapPlugin;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import javax.imageio.ImageIO;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.map.MinecraftFont;


public class NearbyPlayersMapRenderer extends MapRenderer {
    private final int mapRange;
    private final NearbyPlayersMapPlugin plugin;
    private final Map<Player, BufferedImage> playerHeads;
    private MapRenderer vanillaRenderer;
    private Location lastLocation;

    public NearbyPlayersMapRenderer(int mapRange, NearbyPlayersMapPlugin plugin) {
        super(false);
        this.mapRange = mapRange;
        this.plugin = plugin;
        this.playerHeads = new HashMap<>();
        this.vanillaRenderer = null;
        this.lastLocation = null;
    }


    public void render(MapView map, MapCanvas canvas, Player player) {
        if (this.vanillaRenderer == null) {
            for(MapRenderer renderer : map.getRenderers()) {
                if (!(renderer instanceof NearbyPlayersMapRenderer)) {
                    this.vanillaRenderer = renderer;
                    break;
                }
            }
        }

        Location currentLocation = player.getLocation();
        if (this.lastLocation == null || this.lastLocation.distanceSquared(currentLocation) > (double)1.0F) {
            map.setCenterX(currentLocation.getBlockX());
            map.setCenterZ(currentLocation.getBlockZ());
            this.lastLocation = currentLocation;
        }

        if (this.vanillaRenderer != null) {
            this.vanillaRenderer.render(map, canvas, player);
        }

        drawPlayerArrow(canvas, 64, 64, player.getLocation().getYaw());

        for(Player nearbyPlayer : player.getWorld().getPlayers()) {
            if (nearbyPlayer != player && !this.plugin.isPlayerHidden(nearbyPlayer)) {
                int relativeX = (int)(nearbyPlayer.getLocation().getX() - player.getLocation().getX());
                int relativeZ = (int)(nearbyPlayer.getLocation().getZ() - player.getLocation().getZ());
                double distance = Math.sqrt((double)(relativeX * relativeX + relativeZ * relativeZ));
                int mapX;
                int mapZ;
                if (distance <= (double)this.mapRange) {
                    mapX = 64 + relativeX * 64 / this.mapRange;
                    mapZ = 64 + relativeZ * 64 / this.mapRange;
                } else {
                    double angle = Math.atan2((double)relativeZ, (double)relativeX);
                    mapX = 64 + (int)(Math.cos(angle) * (double)63.0F);
                    mapZ = 64 + (int)(Math.sin(angle) * (double)63.0F);
                }

                if (mapX >= 0 && mapX < 128 && mapZ >= 0 && mapZ < 128) {
                    BufferedImage playerHead = (BufferedImage)this.playerHeads.get(nearbyPlayer);
                    if (playerHead == null) {
                        playerHead = this.getPlayerHead(nearbyPlayer);
                        if (playerHead != null) {
                            this.playerHeads.put(nearbyPlayer, playerHead);
                        }
                    }

                    if (playerHead != null) {
                        canvas.drawImage(mapX - 4, mapZ - 4, playerHead.getScaledInstance(8, 8, 1));
                    }

                    String name = nearbyPlayer.getName();
                    int textX = mapX - name.length() * 2;
                    int textY = mapZ + 6;
                    textX = Math.max(0, Math.min(textX, 128 - name.length() * 4));
                    textY = Math.max(0, Math.min(textY, 120));
                    canvas.drawText(textX, textY, MinecraftFont.Font, name);
                }
            }
        }

    }


    private BufferedImage getPlayerHead(Player player) {
        try {
            URL url = new URL("https://minotar.net/avatar/" + player.getName() + "/8");
            return ImageIO.read(url);
        } catch (IOException e) {
            this.plugin.getLogger().log(Level.SEVERE, "Error loading player head image for player: " + player.getName(), e);
            return null;
        }
    }

    private void drawPlayerArrow(MapCanvas canvas, int centerX, int centerY, float yaw) {
        // --- Definición de Colores ---
        // Usaremos MapPalette para obtener los bytes de color más cercanos
        byte colorWhite = MapPalette.WHITE;           // Para la punta de flecha
        byte colorLightGray = MapPalette.LIGHT_GRAY;  // Borde claro de la punta/cola
        byte colorGray = MapPalette.GRAY_2;           // Borde más oscuro de la punta/cola
        byte colorBrown = MapPalette.matchColor(new java.awt.Color(139, 69, 19)); // Marrón (SaddleBrown)
        byte colorDarkBrown = MapPalette.matchColor(new java.awt.Color(101, 67, 33)); // Marrón oscuro (Peru)
        // Nota: matchColor puede no dar el tono exacto, pero busca el más cercano.
        // Alternativamente, puedes probar MapPalette.BROWN, MapPalette.DIRT_BROWN, etc.
        // byte colorBrown = MapPalette.BROWN;
        // byte colorDarkBrown = MapPalette.DIRT_BROWN;


        // --- Definición de la Forma de la Flecha (Píxeles relativos al centro 0,0) ---
        // Y negativo es hacia ARRIBA (Norte en el mapa base)
        // Inspirado en la imagen MwsnNfv.png
        // Formato: {offsetX, offsetY, colorByte}
        byte[][] arrowPixels = {
                // Punta (Head - Blanca con bordes grises)
                {0, -5, colorWhite}, // Punta superior
                {-1, -4, colorWhite},     {0, -4, colorWhite},     {1, -4, colorWhite},
                {-2, -3, colorGray},      {-1, -3, colorWhite},     {0, -3, colorWhite},     {1, -3, colorWhite},     {2, -3, colorGray},
                {-1, -2, colorGray},      {0, -2, colorWhite},     {1, -2, colorGray},

                // Eje (Shaft - Marrón central)
                // {-1, -1, colorDarkBrown}, // Comentado
                {0, -1, colorBrown},
                // {1, -1, colorDarkBrown},  // Comentado
                // {-1, 0, colorDarkBrown},   // Comentado
                {0, 0, colorBrown},      // Fila central
                // {1, 0, colorDarkBrown},   // Comentado
                // {-1, 1, colorDarkBrown},   // Comentado
                {0, 1, colorBrown},
                // {1, 1, colorDarkBrown},   // Comentado
                {0, 2, colorBrown},      // Parte inferior del eje antes de la cola

                // Cola (Tail - Grises)
                {-1, 3, colorGray},       {0, 3, colorLightGray},  {1, 3, colorGray},
                {0, 4, colorGray} // Punta inferior de la cola
        };

        // --- Rotación ---
        // Convertir yaw a ángulo matemático en radianes (0 Este, 90 Norte, 180 Oeste, 270 Sur)
        // Yaw: 0=S, 90=W, 180=N, 270=E
        // Ángulo = (270 - yaw) ajustado a 0-360
        double angleRad = Math.toRadians((180.0f + yaw + 360.0f) % 360.0f);
        double cosTheta = Math.cos(angleRad);
        double sinTheta = Math.sin(angleRad);

        // --- Dibujar Píxeles Rotados ---
        for (byte[] pixelData : arrowPixels) {
            int baseX = pixelData[0];
            int baseY = pixelData[1];
            byte color = pixelData[2];

            // Aplicar rotación 2D (redondeando al píxel más cercano)
            int rotatedX = (int) Math.round(baseX * cosTheta - baseY * sinTheta);
            int rotatedY = (int) Math.round(baseX * sinTheta + baseY * cosTheta);

            // Calcular coordenadas finales en el mapa
            int finalX = centerX + rotatedX;
            int finalY = centerY + rotatedY;

            // Dibujar solo si está dentro de los límites del mapa (0-127)
            if (finalX >= 0 && finalX < 128 && finalY >= 0 && finalY < 128) {
                // Establecer el píxel con su color correspondiente
                canvas.setPixel(finalX, finalY, color);
            }
        }
    }
    }
