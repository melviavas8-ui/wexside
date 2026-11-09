package ru.wexside.render.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import ru.wexside.util.InlineMesh;

public final class MeshBuilder {
    private static final float POSITION_PRECISION = 100000.0f;
    private static final float SMOOTH_EDGE_DOT_THRESHOLD = 0.995f;

    private MeshBuilder() {
    }

    public static InlineMesh create(float[] positions, float[] normals, float[] textureCoordinates, int[] triangleIndices, float[] vertexColors) {
        MeshBuilder.validate(positions, normals, textureCoordinates, triangleIndices, vertexColors);
        float[] positionCopy = Arrays.copyOf(positions, positions.length);
        float[] normalCopy = normals == null ? null : Arrays.copyOf(normals, normals.length);
        float[] textureCopy = textureCoordinates == null ? null : Arrays.copyOf(textureCoordinates, textureCoordinates.length);
        int[] indexCopy = triangleIndices == null ? null : Arrays.copyOf(triangleIndices, triangleIndices.length);
        float[] colorCopy = vertexColors == null ? null : Arrays.copyOf(vertexColors, vertexColors.length);
        return new InlineMesh(positionCopy, normalCopy, textureCopy, indexCopy, MeshBuilder.buildOutlineIndices(positionCopy, indexCopy), colorCopy);
    }

    private static void validate(float[] positions, float[] normals, float[] textureCoordinates, int[] triangleIndices, float[] vertexColors) {
        if (positions == null || positions.length == 0 || positions.length % 3 != 0) {
            throw new IllegalArgumentException("Mesh positions must contain XYZ triplets");
        }
        int vertexCount = positions.length / 3;
        if (normals != null && normals.length != positions.length) {
            throw new IllegalArgumentException("Mesh normals must match the position count");
        }
        if (textureCoordinates != null && textureCoordinates.length != vertexCount * 2) {
            throw new IllegalArgumentException("Mesh texture coordinates must contain one UV pair per vertex");
        }
        if (vertexColors != null && vertexColors.length != vertexCount * 4) {
            throw new IllegalArgumentException("Mesh colors must contain one RGBA value per vertex");
        }
        if (triangleIndices == null) {
            return;
        }
        if (triangleIndices.length == 0 || triangleIndices.length % 3 != 0) {
            throw new IllegalArgumentException("Mesh indices must contain triangle triplets");
        }
        for (int index : triangleIndices) {
            if (index >= 0 && index < vertexCount) continue;
            throw new IllegalArgumentException("Mesh index out of bounds: " + index);
        }
    }

    private static int[] buildOutlineIndices(float[] positions, int[] triangleIndices) {
        HashMap<EdgeKey, EdgeInfo> edges = new HashMap<EdgeKey, EdgeInfo>();
        if (triangleIndices != null) {
            for (int offset = 0; offset < triangleIndices.length; offset += 3) {
                MeshBuilder.addTriangle(edges, positions, triangleIndices[offset], triangleIndices[offset + 1], triangleIndices[offset + 2]);
            }
        } else {
            int vertexCount = positions.length / 3;
            int vertex = 0;
            while (vertex + 2 < vertexCount) {
                MeshBuilder.addTriangle(edges, positions, vertex, vertex + 1, vertex + 2);
                vertex += 3;
            }
        }
        int[] outlineIndices = new int[edges.size() * 2];
        int offset = 0;
        for (EdgeInfo edge : edges.values()) {
            if (!edge.visible()) continue;
            outlineIndices[offset++] = edge.firstIndex;
            outlineIndices[offset++] = edge.secondIndex;
        }
        return Arrays.copyOf(outlineIndices, offset);
    }

    private static void addTriangle(Map<EdgeKey, EdgeInfo> edges, float[] positions, int first, int second, int third) {
        FaceNormal normal = MeshBuilder.calculateNormal(positions, first, second, third);
        MeshBuilder.addEdge(edges, positions, first, second, normal);
        MeshBuilder.addEdge(edges, positions, second, third, normal);
        MeshBuilder.addEdge(edges, positions, third, first, normal);
    }

    private static void addEdge(Map<EdgeKey, EdgeInfo> edges, float[] positions, int firstIndex, int secondIndex, FaceNormal normal) {
        EdgeKey key = EdgeKey.of(MeshBuilder.positionKey(positions, firstIndex), MeshBuilder.positionKey(positions, secondIndex));
        edges.computeIfAbsent(key, ignored -> new EdgeInfo(firstIndex, secondIndex)).addFace(normal);
    }

    private static FaceNormal calculateNormal(float[] positions, int first, int second, int third) {
        int b = second * 3;
        int a = first * 3;
        float abY = positions[b + 1] - positions[a + 1];
        int c = third * 3;
        float acZ = positions[c + 2] - positions[a + 2];
        float abZ = positions[b + 2] - positions[a + 2];
        float acY = positions[c + 1] - positions[a + 1];
        float x = abY * acZ - abZ * acY;
        float acX = positions[c] - positions[a];
        float abX = positions[b] - positions[a];
        float y = abZ * acX - abX * acZ;
        float z = abX * acY - abY * acX;
        float lengthSquared = x * x + y * y + z * z;
        if (lengthSquared <= 1.0E-10f) {
            return new FaceNormal(0.0f, 1.0f, 0.0f);
        }
        float inverseLength = (float)(1.0 / Math.sqrt(lengthSquared));
        return new FaceNormal(x * inverseLength, y * inverseLength, z * inverseLength);
    }

    private static PositionKey positionKey(float[] positions, int vertexIndex) {
        int offset = vertexIndex * 3;
        return new PositionKey(Math.round(positions[offset] * 100000.0f), Math.round(positions[offset + 1] * 100000.0f), Math.round(positions[offset + 2] * 100000.0f));
    }

    private static final class EdgeInfo {
        private final int firstIndex;
        private final int secondIndex;
        private FaceNormal firstNormal;
        private int faceCount;
        private boolean hardEdge;

        private EdgeInfo(int firstIndex, int secondIndex) {
            this.firstIndex = firstIndex;
            this.secondIndex = secondIndex;
        }

        private void addFace(FaceNormal normal) {
            if (this.faceCount == 0) {
                this.firstNormal = normal;
            } else if (this.firstNormal.dot(normal) < 0.995f) {
                this.hardEdge = true;
            }
            ++this.faceCount;
        }

        private boolean visible() {
            return this.faceCount == 1 || this.hardEdge;
        }
    }

    private record FaceNormal(float x, float y, float z) {
        private float dot(FaceNormal other) {
            return this.x * other.x + this.y * other.y + this.z * other.z;
        }
    }

    private record PositionKey(int x, int y, int z) implements Comparable<PositionKey>
    {
        @Override
        public int compareTo(PositionKey other) {
            int xComparison = Integer.compare(this.x, other.x);
            if (xComparison != 0) {
                return xComparison;
            }
            int yComparison = Integer.compare(this.y, other.y);
            return yComparison != 0 ? yComparison : Integer.compare(this.z, other.z);
        }
    }

    private record EdgeKey(PositionKey first, PositionKey second) {
        private static EdgeKey of(PositionKey first, PositionKey second) {
            return first.compareTo(second) <= 0 ? new EdgeKey(first, second) : new EdgeKey(second, first);
        }
    }
}

