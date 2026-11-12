import express from "express";

const app = express();
const port = Number(process.env.PORT || 10000);
const groqApiKey = process.env.GROQ_API_KEY;
const model = process.env.GROQ_MODEL || "llama-3.1-8b-instant";
const maxMessages = 12;

app.disable("x-powered-by");
app.use(express.json({ limit: "32kb" }));

app.get("/health", (_request, response) => {
  response.json({ ok: true, service: "sewa-ai-llm-proxy" });
});

app.post("/v1/chat", async (request, response) => {
  if (!groqApiKey) {
    return response.status(503).json({ error: "LLM service is not configured." });
  }

  const input = request.body?.messages;
  if (!Array.isArray(input) || input.length === 0 || input.length > maxMessages) {
    return response.status(400).json({ error: `messages must contain 1-${maxMessages} items.` });
  }

  const messages = input
    .filter((message) => message && ["user", "assistant"].includes(message.role) && typeof message.content === "string")
    .map((message) => ({ role: message.role, content: message.content.trim().slice(0, 4000) }))
    .filter((message) => message.content.length > 0);

  if (messages.length === 0) {
    return response.status(400).json({ error: "No usable messages were provided." });
  }

  try {
    const groqResponse = await fetch("https://api.groq.com/openai/v1/chat/completions", {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${groqApiKey}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model,
        temperature: 0.4,
        max_tokens: 700,
        messages: [
          {
            role: "system",
            content: "You are SEWA, a concise and helpful personal assistant. Reply in the user's language, using Hindi/Hinglish when they do. Never claim to have performed a phone action; the Android app handles actions locally."
          },
          ...messages
        ]
      })
    });

    const data = await groqResponse.json();
    if (!groqResponse.ok) {
      console.error("Groq request failed:", groqResponse.status, data?.error?.message || "unknown error");
      return response.status(502).json({ error: "Upstream LLM request failed." });
    }

    const reply = data?.choices?.[0]?.message?.content;
    if (typeof reply !== "string" || !reply.trim()) {
      return response.status(502).json({ error: "Upstream LLM returned no text." });
    }
    return response.json({ reply: reply.trim() });
  } catch (error) {
    console.error("LLM proxy error:", error);
    return response.status(502).json({ error: "LLM service request failed." });
  }
});

app.listen(port, "0.0.0.0", () => {
  console.log(`SEWA LLM proxy listening on ${port}`);
});
