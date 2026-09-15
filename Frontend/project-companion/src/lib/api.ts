import { ChatMessage, DeployResponse, FileNode, LoginCredentials, LoginResponse, ProjectSummaryResponse, ProjectRequest, ProjectResponse, ProjectMember, ProjectRole, SignupRequest, AuthResponse } from "./types";

const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

export const getAuthToken = () => localStorage.getItem("auth_token");

export const setAuthToken = (token: string) => localStorage.setItem("auth_token", token);

export const removeAuthToken = () => localStorage.removeItem("auth_token");

export const isAuthenticated = () => !!getAuthToken();

const getAuthHeaders = (): HeadersInit => {
  const token = getAuthToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
};

// User info storage
export const setUserInfo = (user: { id: number; username: string; name: string }) => {
  localStorage.setItem("user_info", JSON.stringify(user));
};

export const getUserInfo = (): { id: number; username: string; name: string } | null => {
  const userInfo = localStorage.getItem("user_info");
  return userInfo ? JSON.parse(userInfo) : null;
};

export const removeUserInfo = () => localStorage.removeItem("user_info");

// LocalStorage keys
export const PREVIEW_URL_KEY = "preview_url";
export const OPEN_TABS_KEY = "open_tabs";
export const ACTIVE_TAB_KEY = "active_tab";

// Helper to parse backend error responses
async function parseErrorMessage(response: Response, defaultMsg: string): Promise<string> {
  try {
    const data = await response.json();
    if (data.message) {
      if (data.errors && Array.isArray(data.errors) && data.errors.length > 0) {
        const fieldErrors = data.errors
          .map((e: { field?: string; message?: string }) => e.message || `${e.field} is invalid`)
          .join(", ");
        return `${data.message}: ${fieldErrors}`;
      }
      return data.message;
    }
  } catch {
    // If not JSON, try text
  }
  try {
    const text = await response.text();
    if (text) return text;
  } catch {
    // ignore
  }
  return defaultMsg;
}

function cleanExtractedCode(raw: string): string {
  if (!raw) return "";
  let clean = raw.trim();
  clean = clean.replace(/^<!\[CDATA\[\s*/i, "").replace(/\s*\]\]>$/i, "");
  clean = clean.replace(/<!\[CDATA\[/gi, "").replace(/\]\]>/gi, "");
  clean = clean.replace(/^```[a-zA-Z0-9_-]*\s*\n/i, "").replace(/\n```\s*$/i, "");
  return clean.trim();
}

// API response format for files endpoint
interface FilesApiResponse {
  files: { path: string }[];
}

// Convert flat file paths to nested tree structure
function buildFileTree(paths: { path: string }[]): FileNode[] {
  const root: FileNode[] = [];
  const nodeMap = new Map<string, FileNode>();

  // Sort paths to ensure directories come before their children
  const sortedPaths = [...paths].sort((a, b) => a.path.localeCompare(b.path));

  for (const { path } of sortedPaths) {
    const parts = path.split("/");
    let currentPath = "";

    for (let i = 0; i < parts.length; i++) {
      const part = parts[i];
      const parentPath = currentPath;
      currentPath = currentPath ? `${currentPath}/${part}` : part;

      // Skip if node already exists
      if (nodeMap.has(currentPath)) continue;

      const isFile = i === parts.length - 1;
      const node: FileNode = {
        name: part,
        path: currentPath,
        type: isFile ? "file" : "directory",
        children: isFile ? undefined : [],
      };

      nodeMap.set(currentPath, node);

      if (parentPath) {
        const parent = nodeMap.get(parentPath);
        if (parent && parent.children) {
          parent.children.push(node);
        }
      } else {
        root.push(node);
      }
    }
  }

  // Sort each level: directories first, then alphabetically
  const sortNodes = (nodes: FileNode[]) => {
    nodes.sort((a, b) => {
      if (a.type === "directory" && b.type === "file") return -1;
      if (a.type === "file" && b.type === "directory") return 1;
      return a.name.localeCompare(b.name);
    });
    nodes.forEach((node) => {
      if (node.children) sortNodes(node.children);
    });
  };

  sortNodes(root);
  return root;
}

export const api = {
  async login(credentials: LoginCredentials): Promise<LoginResponse> {
    const response = await fetch(`${BASE_URL}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(credentials),
    });

    if (!response.ok) {
      const errorMsg = await parseErrorMessage(response, "Login failed");
      throw new Error(errorMsg);
    }

    return response.json();
  },

  async signup(data: SignupRequest): Promise<AuthResponse> {
    const response = await fetch(`${BASE_URL}/api/auth/signup`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(data),
    });

    if (!response.ok) {
      const errorMsg = await parseErrorMessage(response, "Signup failed");
      throw new Error(errorMsg);
    }

    return response.json();
  },

  async getFiles(projectId: string): Promise<FileNode[]> {
    const response = await fetch(`${BASE_URL}/api/projects/${projectId}/files`, {
      headers: { ...getAuthHeaders() },
    });

    if (!response.ok) {
      const errorMsg = await parseErrorMessage(response, "Failed to fetch files");
      throw new Error(errorMsg);
    }

    const data: FilesApiResponse = await response.json();
    return buildFileTree(data.files || []);
  },

  async getFileContent(projectId: string, path: string): Promise<string> {
    const response = await fetch(
      `${BASE_URL}/api/projects/${projectId}/files/content?path=${encodeURIComponent(path)}`,
      {
        headers: { ...getAuthHeaders() },
      }
    );

    if (!response.ok) {
      const errorMsg = await parseErrorMessage(response, "Failed to fetch file content");
      throw new Error(errorMsg);
    }

    const data = await response.json();
    return data.content;
  },

  async deploy(projectId: string): Promise<DeployResponse> {
    const response = await fetch(`${BASE_URL}/api/projects/${projectId}/deploy`, {
      method: "POST",
      headers: { ...getAuthHeaders() },
    });

    if (!response.ok) {
      const errorMsg = await parseErrorMessage(response, "Deployment failed");
      throw new Error(errorMsg);
    }

    return response.json();
  },

  async getProjects(): Promise<ProjectSummaryResponse[]> {
    const response = await fetch(`${BASE_URL}/api/projects`, {
      headers: { ...getAuthHeaders() },
    });

    if (!response.ok) {
      const errorMsg = await parseErrorMessage(response, "Failed to fetch projects");
      throw new Error(errorMsg);
    }

    return response.json();
  },

  async createProject(name: string): Promise<ProjectSummaryResponse> {
    const response = await fetch(`${BASE_URL}/api/projects`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...getAuthHeaders() },
      body: JSON.stringify({ name }),
    });

    if (!response.ok) {
      const errorMsg = await parseErrorMessage(response, "Failed to create project");
      throw new Error(errorMsg);
    }

    return response.json();
  },

  async getProject(id: string): Promise<ProjectResponse> {
    const response = await fetch(`${BASE_URL}/api/projects/${id}`, {
      headers: { ...getAuthHeaders() },
    });

    if (!response.ok) {
      const errorMsg = await parseErrorMessage(response, "Failed to fetch project");
      throw new Error(errorMsg);
    }

    return response.json();
  },

  async updateProject(id: string, name: string): Promise<ProjectResponse> {
    const response = await fetch(`${BASE_URL}/api/projects/${id}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json", ...getAuthHeaders() },
      body: JSON.stringify({ name }),
    });

    if (!response.ok) {
      const errorMsg = await parseErrorMessage(response, "Failed to update project");
      throw new Error(errorMsg);
    }

    return response.json();
  },

  async deleteProject(id: string): Promise<void> {
    const response = await fetch(`${BASE_URL}/api/projects/${id}`, {
      method: "DELETE",
      headers: { ...getAuthHeaders() },
    });

    if (!response.ok) {
      const errorMsg = await parseErrorMessage(response, "Failed to delete project");
      throw new Error(errorMsg);
    }
  },

  async downloadProjectZip(id: string): Promise<Blob> {
    const response = await fetch(`${BASE_URL}/api/projects/${id}/files/download-zip`, {
      headers: { ...getAuthHeaders() },
    });

    if (!response.ok) {
      const errorMsg = await parseErrorMessage(response, "Failed to download project");
      throw new Error(errorMsg);
    }

    return response.blob();
  },

  async getProjectMembers(projectId: string): Promise<ProjectMember[]> {
    const response = await fetch(`${BASE_URL}/api/projects/${projectId}/members`, {
      headers: { ...getAuthHeaders() },
    });

    if (!response.ok) {
      const errorMsg = await parseErrorMessage(response, "Failed to fetch project members");
      throw new Error(errorMsg);
    }

    return response.json();
  },

  async inviteMember(projectId: string, username: string, role: ProjectRole): Promise<void> {
    const response = await fetch(`${BASE_URL}/api/projects/${projectId}/members`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...getAuthHeaders() },
      body: JSON.stringify({ username, role }),
    });

    if (!response.ok) {
      const errorMsg = await parseErrorMessage(response, "Failed to invite member");
      throw new Error(errorMsg);
    }
  },

  async updateMemberRole(projectId: string, userId: number, role: ProjectRole): Promise<void> {
    const response = await fetch(`${BASE_URL}/api/projects/${projectId}/members/${userId}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json", ...getAuthHeaders() },
      body: JSON.stringify({ role }),
    });

    if (!response.ok) {
      const errorMsg = await parseErrorMessage(response, "Failed to update member role");
      throw new Error(errorMsg);
    }
  },

  async removeMember(projectId: string, userId: number): Promise<void> {
    const response = await fetch(`${BASE_URL}/api/projects/${projectId}/members/${userId}`, {
      method: "DELETE",
      headers: { ...getAuthHeaders() },
    });

    if (!response.ok) {
      const errorMsg = await parseErrorMessage(response, "Failed to remove member");
      throw new Error(errorMsg);
    }
  },

  async getChatHistory(projectId: string): Promise<ChatMessage[]> {
    const response = await fetch(`${BASE_URL}/api/chat/projects/${projectId}`, {
      headers: { ...getAuthHeaders() },
    });

    if (!response.ok) {
      const errorMsg = await parseErrorMessage(response, "Failed to fetch chat history");
      throw new Error(errorMsg);
    }

    return response.json();
  },

  async streamChat(
    projectId: string,
    message: string,
    onChunk: (chunk: string) => void,
    onFile: (path: string, content: string) => void,
    onComplete: () => void,
    onError: (error: Error) => void
  ) {
    const controller = new AbortController();

    fetch(`${BASE_URL}/api/chat/stream`, {
      method: "POST",
      headers: { "Content-Type": "application/json", ...getAuthHeaders() },
      body: JSON.stringify({ message, projectId }),
      signal: controller.signal,
    })
      .then(async (response) => {
        if (!response.ok) throw new Error("Chat stream failed");

        const reader = response.body?.getReader();
        if (!reader) throw new Error("No reader available");

        const decoder = new TextDecoder();

        // Buffers
        let sseBuffer = ""; // To handle split SSE lines
        let fullContentBuffer = ""; // To accumulate clean text for file regex
        // let lastProcessedIndex = 0; // Optimization for regex

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          const chunk = decoder.decode(value, { stream: true });
          sseBuffer += chunk;

          // Process line by line to handle SSE format (data: ...)
          const lines = sseBuffer.split("\n");
          sseBuffer = lines.pop() || "";

          for (const line of lines) {
            const trimmedLine = line.trim();
            if (!trimmedLine || !trimmedLine.startsWith("data:")) continue;

            const dataStr = trimmedLine.slice(5).trim();
            if (!dataStr) continue;

            try {
              // FIX: Parse JSON to get the real text with newlines preserved
              const parsed = JSON.parse(dataStr);
              const content = parsed.text;

              // 1. Send clean text to UI
              onChunk(content);

              // 2. Accumulate for file parsing
              fullContentBuffer += content;

              // Parse completed or streaming <file> tags
              const fileRegex = /<file\s+path="([^"]+)">([\s\S]*?)(?:<\/file>|$)/gi;
              let fileMatch: RegExpExecArray | null;
              while ((fileMatch = fileRegex.exec(fullContentBuffer)) !== null) {
                const filePath = fileMatch[1];
                const fileBody = fileMatch[2];
                if (filePath && fileBody) {
                  onFile(filePath, cleanExtractedCode(fileBody));
                }
              }
            } catch (e) {
              console.error("Failed to parse SSE JSON:", e);
            }
          }
        }

        // Final extraction on completion
        const finalRegex = /<file\s+path="([^"]+)">([\s\S]*?)<\/file>/gi;
        let finalMatch: RegExpExecArray | null;
        while ((finalMatch = finalRegex.exec(fullContentBuffer)) !== null) {
          const filePath = finalMatch[1];
          const fileBody = finalMatch[2];
          if (filePath && fileBody) {
            onFile(filePath, cleanExtractedCode(fileBody));
          }
        }

        onComplete();
      })
      .catch((error) => {
        if (error.name !== "AbortError") {
          console.error("Stream error:", error);
          onError(error);
        }
      });

    return () => controller.abort();
  }

};
