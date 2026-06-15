import { useState } from "react";
import "./App.css";

type SummaryResponse = {
  id: number;
  summary: string;
  model: string;
  cached: boolean;
  createdAt: string;
};

function App() {
  const [text, setText] = useState("");
  const [response, setResponse] = useState<SummaryResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [elapsed, setElapsed] = useState<number | null>(null);

  async function handleSummarize() {
    setLoading(true);
    setError(null);
    setResponse(null);
    setElapsed(null);

    const start = performance.now();

    try {
      const res = await fetch("http://localhost:8080/api/summarize", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ text }),
      });

      if (!res.ok) {
        const body = await res.text();
        throw new Error(`${res.status}: ${body}`);
      }

      const data: SummaryResponse = await res.json();
      setResponse(data);
      setElapsed(Math.round(performance.now() - start));
    } catch (e: unknown) {
      setError(e instanceof Error ? e.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  }

  const wordCount = text.trim().split(/\s+/).filter(Boolean).length;
  const tooShort = text.length < 50;

  return (
    <div className="container">
      <header>
        <h1>AI Content Summarizer</h1>
        <p className="subtitle">
          Three-tier cache: Redis → Postgres → Gemini. Identical inputs are
          served from cache after the first call.
        </p>
      </header>

      <div className="grid">
        <section className="panel">
          <label htmlFor="text" className="panel-label">
            Input text
          </label>
          <textarea
            id="text"
            placeholder="Paste text here (minimum 50 characters)..."
            value={text}
            onChange={(e) => setText(e.target.value)}
            rows={14}
          />
          <div className="row between">
            <span className="muted">
              {text.length} chars · {wordCount} words
            </span>
            <button
              onClick={handleSummarize}
              disabled={loading || tooShort}
              className="primary"
            >
              {loading ? "Summarizing..." : "Summarize"}
            </button>
          </div>
          {tooShort && text.length > 0 && (
            <p className="hint">
              Need at least 50 characters ({50 - text.length} to go)
            </p>
          )}
        </section>

        <section className="panel">
          <label className="panel-label">Summary</label>

          {!response && !error && !loading && (
            <div className="empty">Summary will appear here.</div>
          )}

          {loading && <div className="empty">Calling backend...</div>}

          {error && (
            <div className="error">
              <strong>Error:</strong> {error}
            </div>
          )}

          {response && (
            <div className="result">
              <div className="badges">
                <span
                  className={`badge ${response.cached ? "cached" : "fresh"}`}
                >
                  {response.cached ? "Cached" : "Fresh from Gemini"}
                </span>
                <span className="badge neutral">{response.model}</span>
                {elapsed !== null && (
                  <span className="badge neutral">{elapsed}ms</span>
                )}
              </div>
              <p className="summary-text">{response.summary}</p>
              <div className="meta">
                ID: {response.id} · {new Date(response.createdAt).toLocaleString()}
              </div>
            </div>
          )}
        </section>
      </div>

      <footer>
        Tip: click <em>Summarize</em> twice with the same text to see the cache
        kick in.
      </footer>
    </div>
  );
}

export default App;