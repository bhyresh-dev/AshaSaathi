"""
Generates a minimal dummy risk_model.tflite for dev/CI use.
Input:  [1, 20] float32  (20 clinical features)
Output: [1, 3]  float32  (GREEN / YELLOW / RED probabilities)

Usage:
    pip install tensorflow
    python scripts/generate_dummy_tflite.py
"""
import numpy as np

try:
    import tensorflow as tf

    inp = tf.keras.Input(shape=(20,), name="features")
    x   = tf.keras.layers.Dense(16, activation="relu")(inp)
    out = tf.keras.layers.Dense(3,  activation="softmax", name="risk_probs")(x)
    model = tf.keras.Model(inp, out)
    model.compile()

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    tflite_bytes = converter.convert()

    out_path = "app/src/main/assets/risk_model.tflite"
    with open(out_path, "wb") as f:
        f.write(tflite_bytes)

    print(f"Written {len(tflite_bytes):,} bytes → {out_path}")
    print("Input  shape: [1, 20] float32")
    print("Output shape: [1,  3] float32  (index 2 = RED probability)")

except ImportError:
    # Fallback: write a minimal hand-crafted TFLite flatbuffer stub
    # This is NOT a valid model for inference but keeps the asset slot filled.
    # Replace with the real script output before release.
    stub = b"STUB_TFLITE_REPLACE_WITH_REAL_MODEL"
    out_path = "app/src/main/assets/risk_model.tflite"
    with open(out_path, "wb") as f:
        f.write(stub)
    print(f"TensorFlow not found. Wrote stub placeholder → {out_path}")
    print("Run: pip install tensorflow && python scripts/generate_dummy_tflite.py")
