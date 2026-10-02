import { useState, useEffect, useRef } from "react";
import { Play, Loader2, ExternalLink, RefreshCw, Globe } from "lucide-react";
import { Button } from "@/components/ui/button";
import { api, PREVIEW_URL_KEY } from "@/lib/api";
import { useToast } from "@/hooks/use-toast";

import { RuntimeErrorAlert, RuntimeError } from "@/components/RuntimeErrorAlert";

interface PreviewPanelProps {
  projectId: string;
  runtimeError: RuntimeError | null;
  onDismiss: () => void;
  onFix: (error: RuntimeError) => void;
  refreshTrigger?: number;
}

export function PreviewPanel({ projectId, runtimeError, onDismiss, onFix, refreshTrigger }: PreviewPanelProps) {
  const previewUrlKey = `${PREVIEW_URL_KEY}_${projectId}`;
  const iframeRef = useRef<HTMLIFrameElement>(null);
  const defaultPreviewUrl = `${import.meta.env.VITE_API_URL || "http://localhost:8080"}/preview/${projectId}`;

  const [previewUrl, setPreviewUrl] = useState<string>(() => {
    // Load from localStorage on mount or fallback to default
    return localStorage.getItem(previewUrlKey) || defaultPreviewUrl;
  });
  const [urlInput, setUrlInput] = useState<string>(() => {
    return localStorage.getItem(previewUrlKey) || defaultPreviewUrl;
  });
  const [isDeploying, setIsDeploying] = useState(false);
  const { toast } = useToast();

  // Reload iframe when refreshTrigger updates
  useEffect(() => {
    if (refreshTrigger && refreshTrigger > 0 && iframeRef.current) {
      const currentSrc = iframeRef.current.src;
      // Force iframe to reload by resetting src
      iframeRef.current.src = "";
      setTimeout(() => {
        if (iframeRef.current) {
          iframeRef.current.src = currentSrc;
        }
      }, 50);
    }
  }, [refreshTrigger]);

  // Sync previewUrl state when projectId changes
  useEffect(() => {
    const storedUrl = localStorage.getItem(previewUrlKey);
    const targetUrl = storedUrl || defaultPreviewUrl;
    setPreviewUrl(targetUrl);
    setUrlInput(targetUrl);
  }, [projectId, previewUrlKey, defaultPreviewUrl]);

  // Store previewUrl in localStorage when it changes
  useEffect(() => {
    if (previewUrl) {
      localStorage.setItem(previewUrlKey, previewUrl);
    }
  }, [previewUrl, previewUrlKey]);

  const handleUrlSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    let targetUrl = urlInput.trim();
    if (targetUrl && !/^https?:\/\//i.test(targetUrl)) {
      targetUrl = `http://${targetUrl}`;
    }
    setPreviewUrl(targetUrl || null);
    if (targetUrl) {
      localStorage.setItem(previewUrlKey, targetUrl);
      toast({
        title: "Preview URL updated",
        description: `Connected to ${targetUrl}`,
      });
    }
  };

  const handleDeploy = async () => {
    setIsDeploying(true);

    try {
      const response = await api.deploy(projectId);
      setPreviewUrl(response.previewUrl);
      setUrlInput(response.previewUrl);
      toast({
        title: "Deployment successful",
        description: "Your preview is now ready",
      });
    } catch (error) {
      toast({
        title: "Deployment failed",
        description: error instanceof Error ? error.message : "Something went wrong",
        variant: "destructive",
      });
    } finally {
      setIsDeploying(false);
    }
  };

  const handleRefresh = () => {
    if (iframeRef.current) {
      const currentSrc = iframeRef.current.src;
      iframeRef.current.src = currentSrc;
    }
  };

  return (
    <div className="flex flex-col h-full bg-background">
      {/* URL Bar */}
      <div className="h-12 shrink-0 flex items-center gap-2 px-3 border-b border-border/50 bg-panel">
        <div className="flex items-center gap-1">
          <Button
            variant="ghost"
            size="icon"
            onClick={handleRefresh}
            disabled={!previewUrl}
            className="h-7 w-7 text-muted-foreground hover:text-foreground"
          >
            <RefreshCw className="w-3.5 h-3.5" />
          </Button>
        </div>

        <form 
          onSubmit={handleUrlSubmit}
          className="flex-1 flex items-center h-8 px-2.5 rounded-md bg-muted/50 border border-border/40 focus-within:border-primary/50 transition-colors"
        >
          <Globe className="w-3.5 h-3.5 mr-2 shrink-0 text-muted-foreground" />
          <input
            type="text"
            value={urlInput}
            onChange={(e) => setUrlInput(e.target.value)}
            placeholder="Enter preview URL (e.g. http://localhost:5174) & press Enter"
            className="w-full bg-transparent border-none outline-none text-xs text-foreground placeholder:text-muted-foreground/50 font-mono"
          />
        </form>

        <div className="flex items-center gap-1">
          {previewUrl && (
            <Button
              variant="ghost"
              size="icon"
              onClick={() => window.open(previewUrl, "_blank")}
              className="h-7 w-7 text-muted-foreground hover:text-foreground"
            >
              <ExternalLink className="w-3.5 h-3.5" />
            </Button>
          )}
          <Button
            onClick={handleDeploy}
            disabled={isDeploying}
            size="sm"
            className="h-7 px-3 bg-primary hover:bg-primary/90 text-xs font-medium"
          >
            {isDeploying ? (
              <>
                <Loader2 className="w-3 h-3 mr-1.5 animate-spin" />
                Deploying
              </>
            ) : (
              <>
                <Play className="w-3 h-3 mr-1.5" />
                Run Preview
              </>
            )}
          </Button>
        </div>
      </div>

      {/* Preview Area */}
      <div className="flex-1 bg-[#1a1a1a]">
        {previewUrl ? (
          <iframe
            ref={iframeRef}
            src={previewUrl}
            className="w-full h-full border-0"
            title="Preview"
            sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
          />
        ) : (
          <div className="flex flex-col items-center justify-center h-full text-center p-8">
            <div className="w-16 h-16 rounded-xl bg-muted/20 flex items-center justify-center mb-4">
              <Globe className="w-8 h-8 text-muted-foreground/50" />
            </div>
            <p className="text-sm text-muted-foreground">
              No preview available yet
            </p>
          </div>
        )}
      </div>

      {/* Error Alert Overlay - Inside the Preview Panel */}
      <RuntimeErrorAlert
        error={runtimeError}
        onDismiss={onDismiss}
        onFix={onFix}
      />
    </div>
  );
}
