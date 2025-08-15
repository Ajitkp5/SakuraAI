import express from 'express';
import bodyParser from 'body-parser';

const app = express();
app.use(bodyParser.json());
const port = 3000;

app.post('/api/chat', async (req, res) => {
  try {
    const prompt = (req.body && req.body.prompt) ? String(req.body.prompt) : "";
    const key = process.env.OPENAI_KEY;
    let reply = "";
    if (!key) {
      reply = "(no key) You said: " + prompt;
    } else {
      const r = await fetch("https://api.openai.com/v1/chat/completions", {
        method: "POST",
        headers: {
          "Authorization": "Bearer " + key,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          model: "gpt-4o-mini",
          messages: [
            {role:"system", content:"You are Sakura, a bilingual (Hindi+English) assistant."},
            {role:"user", content: prompt}
          ]
        })
      });
      const j = await r.json();
      reply = (j.choices?.[0]?.message?.content) || "Sorry, no reply";
    }
    res.json({ reply });
  } catch (e) {
    res.status(500).json({ error: String(e) });
  }
});

app.listen(port, () => console.log(`Sakura backend on ${port}`));