package com.flowboard.workspace.service;

import com.flowboard.workspace.dto.*;
import com.flowboard.workspace.entity.Workspace;
import com.flowboard.workspace.entity.WorkspaceMember;
import com.flowboard.workspace.exception.AppException;
import com.flowboard.workspace.repository.WorkspaceMemberRepository;
import com.flowboard.workspace.repository.WorkspaceRepository;
import com.flowboard.workspace.service.impl.WorkspaceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkspaceServiceImpl Unit Tests")
class WorkspaceServiceImplTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberRepository memberRepository;

    @InjectMocks private WorkspaceServiceImpl workspaceService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private Workspace workspace;
    private WorkspaceMember ownerMember;
    private WorkspaceMember regularMember;

    @BeforeEach
    void setUp() {
        workspace = Workspace.builder()
                .workspaceId(1)
                .name("Test Workspace")
                .description("A test workspace")
                .ownerId(10)
                .visibility(Workspace.Visibility.PRIVATE)
                .logoUrl(null)
                .build();

        ownerMember = WorkspaceMember.builder()
                .memberId(1)
                .workspaceId(1)
                .userId(10)
                .role(WorkspaceMember.Role.ADMIN)
                .build();

        regularMember = WorkspaceMember.builder()
                .memberId(2)
                .workspaceId(1)
                .userId(20)
                .role(WorkspaceMember.Role.MEMBER)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createWorkspace()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createWorkspace()")
    class CreateWorkspaceTests {

        @Test
        @DisplayName("should create workspace and auto-add owner as ADMIN member")
        void createWorkspace_success() {
            WorkspaceRequest req = new WorkspaceRequest();
            req.setName("Test Workspace");
            req.setDescription("A test workspace");
            req.setVisibility("PRIVATE");

            when(workspaceRepository.existsByNameAndOwnerId("Test Workspace", 10))
                    .thenReturn(false);
            when(workspaceRepository.save(any(Workspace.class)))
                    .thenAnswer(inv -> {
                        Workspace w = inv.getArgument(0);
                        w.setWorkspaceId(1);
                        return w;
                    });
            when(memberRepository.save(any(WorkspaceMember.class)))
                    .thenReturn(ownerMember);
            when(memberRepository.findByWorkspaceId(1))
                    .thenReturn(List.of(ownerMember));

            WorkspaceResponse response = workspaceService.createWorkspace(req, 10);

            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("Test Workspace");
            assertThat(response.getOwnerId()).isEqualTo(10);
            assertThat(response.getVisibility()).isEqualTo("PRIVATE");

            // Verify owner was added as ADMIN member
            ArgumentCaptor<WorkspaceMember> memberCaptor =
                    ArgumentCaptor.forClass(WorkspaceMember.class);
            verify(memberRepository).save(memberCaptor.capture());
            assertThat(memberCaptor.getValue().getRole())
                    .isEqualTo(WorkspaceMember.Role.ADMIN);
            assertThat(memberCaptor.getValue().getUserId()).isEqualTo(10);
        }

        @Test
        @DisplayName("should throw CONFLICT when workspace name already exists for owner")
        void createWorkspace_duplicateName_throwsConflict() {
            WorkspaceRequest req = new WorkspaceRequest();
            req.setName("Test Workspace");
            req.setVisibility("PRIVATE");

            when(workspaceRepository.existsByNameAndOwnerId("Test Workspace", 10))
                    .thenReturn(true);

            AppException ex = catchThrowableOfType(
                    () -> workspaceService.createWorkspace(req, 10),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(ex.getMessage())
                    .isEqualTo("Workspace with this name already exists");
            verify(workspaceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should create PUBLIC workspace when visibility is PUBLIC")
        void createWorkspace_publicVisibility() {
            WorkspaceRequest req = new WorkspaceRequest();
            req.setName("Public WS");
            req.setVisibility("PUBLIC");

            when(workspaceRepository.existsByNameAndOwnerId(any(), any()))
                    .thenReturn(false);
            when(workspaceRepository.save(any())).thenAnswer(inv -> {
                Workspace w = inv.getArgument(0);
                w.setWorkspaceId(2);
                return w;
            });
            when(memberRepository.save(any())).thenReturn(ownerMember);
            when(memberRepository.findByWorkspaceId(2)).thenReturn(List.of());

            WorkspaceResponse resp = workspaceService.createWorkspace(req, 10);

            ArgumentCaptor<Workspace> wsCaptor =
                    ArgumentCaptor.forClass(Workspace.class);
            verify(workspaceRepository).save(wsCaptor.capture());
            assertThat(wsCaptor.getValue().getVisibility())
                    .isEqualTo(Workspace.Visibility.PUBLIC);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getById()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getById()")
    class GetByIdTests {

        @Test
        @DisplayName("should return workspace response when found")
        void getById_found() {
            when(workspaceRepository.findById(1))
                    .thenReturn(Optional.of(workspace));
            when(memberRepository.findByWorkspaceId(1))
                    .thenReturn(List.of(ownerMember));

            WorkspaceResponse resp = workspaceService.getById(1);

            assertThat(resp.getWorkspaceId()).isEqualTo(1);
            assertThat(resp.getName()).isEqualTo("Test Workspace");
            assertThat(resp.getMemberCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when workspace does not exist")
        void getById_notFound() {
            when(workspaceRepository.findById(99))
                    .thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> workspaceService.getById(99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getMessage()).isEqualTo("Workspace not found");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getByOwner() / getByMember() / getMyWorkspaces()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Workspace listing queries")
    class ListingTests {

        @Test
        @DisplayName("getByOwner() should return workspaces owned by user")
        void getByOwner_returnsOwned() {
            when(workspaceRepository.findByOwnerId(10))
                    .thenReturn(List.of(workspace));
            when(memberRepository.findByWorkspaceId(1))
                    .thenReturn(List.of(ownerMember));

            List<WorkspaceResponse> result = workspaceService.getByOwner(10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOwnerId()).isEqualTo(10);
        }

        @Test
        @DisplayName("getByMember() should return workspaces user is member of")
        void getByMember_returnsMemberOf() {
            when(memberRepository.findWorkspaceIdsByUserId(20))
                    .thenReturn(List.of(1));
            when(workspaceRepository.findAllById(List.of(1)))
                    .thenReturn(List.of(workspace));
            when(memberRepository.findByWorkspaceId(1))
                    .thenReturn(List.of(ownerMember, regularMember));

            List<WorkspaceResponse> result = workspaceService.getByMember(20);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMemberCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("getPublicWorkspaces() should return only PUBLIC workspaces")
        void getPublicWorkspaces() {
            Workspace publicWs = Workspace.builder()
                    .workspaceId(2).name("Public WS").ownerId(10)
                    .visibility(Workspace.Visibility.PUBLIC).build();

            when(workspaceRepository.findByVisibility(Workspace.Visibility.PUBLIC))
                    .thenReturn(List.of(publicWs));
            when(memberRepository.findByWorkspaceId(2)).thenReturn(List.of());

            List<WorkspaceResponse> result = workspaceService.getPublicWorkspaces();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getVisibility()).isEqualTo("PUBLIC");
        }

        @Test
        @DisplayName("getMyWorkspaces() should merge owned and member workspaces without duplicates")
        void getMyWorkspaces_deduplicates() {
            // User 10 owns workspace 1 and is also a member of it
            when(workspaceRepository.findByOwnerId(10))
                    .thenReturn(List.of(workspace));
            when(memberRepository.findWorkspaceIdsByUserId(10))
                    .thenReturn(List.of(1)); // same workspace
            when(workspaceRepository.findAllById(List.of(1)))
                    .thenReturn(List.of(workspace));
            when(memberRepository.findByWorkspaceId(1))
                    .thenReturn(List.of(ownerMember));

            List<WorkspaceResponse> result = workspaceService.getMyWorkspaces(10);

            // Should not duplicate — only 1 result
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("getAllWorkspaces() should return all workspaces")
        void getAllWorkspaces() {
            when(workspaceRepository.findAll()).thenReturn(List.of(workspace));
            when(memberRepository.findByWorkspaceId(1))
                    .thenReturn(List.of(ownerMember));

            List<WorkspaceResponse> result = workspaceService.getAllWorkspaces();

            assertThat(result).hasSize(1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateWorkspace()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateWorkspace()")
    class UpdateWorkspaceTests {

        @Test
        @DisplayName("owner should be able to update workspace")
        void updateWorkspace_byOwner_success() {
            WorkspaceRequest req = new WorkspaceRequest();
            req.setName("Updated Name");
            req.setVisibility("PUBLIC");

            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
            when(workspaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(memberRepository.findByWorkspaceId(1)).thenReturn(List.of(ownerMember));

            WorkspaceResponse resp = workspaceService.updateWorkspace(1, req, 10, "MEMBER");

            assertThat(resp.getName()).isEqualTo("Updated Name");
            assertThat(resp.getVisibility()).isEqualTo("PUBLIC");
        }

        @Test
        @DisplayName("PLATFORM_ADMIN should update any workspace without ownership check")
        void updateWorkspace_byPlatformAdmin_success() {
            WorkspaceRequest req = new WorkspaceRequest();
            req.setName("Admin Updated");
            req.setVisibility("PRIVATE");

            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
            when(workspaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(memberRepository.findByWorkspaceId(1)).thenReturn(List.of());

            // requesterId=99 is not the owner, but role is PLATFORM_ADMIN
            WorkspaceResponse resp =
                    workspaceService.updateWorkspace(1, req, 99, "PLATFORM_ADMIN");

            assertThat(resp.getName()).isEqualTo("Admin Updated");
            // No ownership check — memberRepository.findByWorkspaceIdAndUserId NOT called
            verify(memberRepository, never()).findByWorkspaceIdAndUserId(any(), eq(99));
        }

        @Test
        @DisplayName("non-owner non-admin should throw FORBIDDEN")
        void updateWorkspace_byStranger_throwsForbidden() {
            WorkspaceRequest req = new WorkspaceRequest();
            req.setName("Hacked Name");
            req.setVisibility("PUBLIC");

            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
            // User 99 is not owner and not admin
            when(memberRepository.findByWorkspaceIdAndUserId(1, 99))
                    .thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> workspaceService.updateWorkspace(1, req, 99, "MEMBER"),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("null fields in request should not overwrite existing values")
        void updateWorkspace_nullFieldsIgnored() {
            WorkspaceRequest req = new WorkspaceRequest();
            req.setName(null);
            req.setDescription(null);
            req.setVisibility(null);

            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
            when(workspaceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(memberRepository.findByWorkspaceId(1)).thenReturn(List.of());

            WorkspaceResponse resp = workspaceService.updateWorkspace(1, req, 10, "MEMBER");

            assertThat(resp.getName()).isEqualTo("Test Workspace");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // deleteWorkspace()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteWorkspace()")
    class DeleteWorkspaceTests {

        @Test
        @DisplayName("owner should delete workspace and all its members")
        void deleteWorkspace_byOwner_success() {
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
            when(memberRepository.findByWorkspaceId(1))
                    .thenReturn(List.of(ownerMember, regularMember));

            workspaceService.deleteWorkspace(1, 10, "MEMBER");

            verify(memberRepository, times(2)).delete(any(WorkspaceMember.class));
            verify(workspaceRepository).delete(workspace);
        }

        @Test
        @DisplayName("PLATFORM_ADMIN should delete any workspace")
        void deleteWorkspace_byPlatformAdmin_success() {
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
            when(memberRepository.findByWorkspaceId(1)).thenReturn(List.of(ownerMember));

            workspaceService.deleteWorkspace(1, 99, "PLATFORM_ADMIN");

            verify(workspaceRepository).delete(workspace);
        }

        @Test
        @DisplayName("non-owner should throw FORBIDDEN on delete")
        void deleteWorkspace_byStranger_throwsForbidden() {
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
            when(memberRepository.findByWorkspaceIdAndUserId(1, 99))
                    .thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> workspaceService.deleteWorkspace(1, 99, "MEMBER"),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
            verify(workspaceRepository, never()).delete(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // addMember()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addMember()")
    class AddMemberTests {

        @Test
        @DisplayName("should add a new member successfully")
        void addMember_success() {
            AddMemberRequest req = new AddMemberRequest();
            req.setUserId(30);
            req.setRole("MEMBER");

            WorkspaceMember newMember = WorkspaceMember.builder()
                    .memberId(3).workspaceId(1).userId(30)
                    .role(WorkspaceMember.Role.MEMBER).build();

            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
            when(memberRepository.existsByWorkspaceIdAndUserId(1, 30))
                    .thenReturn(false);
            when(memberRepository.save(any())).thenReturn(newMember);

            WorkspaceMember result = workspaceService.addMember(1, req, 10);

            assertThat(result.getUserId()).isEqualTo(30);
            assertThat(result.getRole()).isEqualTo(WorkspaceMember.Role.MEMBER);
        }

        @Test
        @DisplayName("should throw CONFLICT when user is already a member")
        void addMember_alreadyMember_throwsConflict() {
            AddMemberRequest req = new AddMemberRequest();
            req.setUserId(20);
            req.setRole("MEMBER");

            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
            when(memberRepository.existsByWorkspaceIdAndUserId(1, 20))
                    .thenReturn(true);

            AppException ex = catchThrowableOfType(
                    () -> workspaceService.addMember(1, req, 10),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(ex.getMessage()).isEqualTo("User is already a member");
        }

        @Test
        @DisplayName("non-owner non-admin should throw FORBIDDEN when adding member")
        void addMember_byStranger_throwsForbidden() {
            AddMemberRequest req = new AddMemberRequest();
            req.setUserId(30);
            req.setRole("MEMBER");

            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
            when(memberRepository.findByWorkspaceIdAndUserId(1, 99))
                    .thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> workspaceService.addMember(1, req, 99),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // removeMember()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("removeMember()")
    class RemoveMemberTests {

        @Test
        @DisplayName("owner should remove a regular member")
        void removeMember_success() {
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));

            workspaceService.removeMember(1, 20, 10);

            verify(memberRepository).deleteByWorkspaceIdAndUserId(1, 20);
        }

        @Test
        @DisplayName("should throw BAD_REQUEST when trying to remove the owner")
        void removeMember_owner_throwsBadRequest() {
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));

            AppException ex = catchThrowableOfType(
                    () -> workspaceService.removeMember(1, 10, 10),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(ex.getMessage()).isEqualTo("Cannot remove workspace owner");
            verify(memberRepository, never()).deleteByWorkspaceIdAndUserId(any(), any());
        }

        @Test
        @DisplayName("non-owner non-admin should throw FORBIDDEN")
        void removeMember_byStranger_throwsForbidden() {
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
            when(memberRepository.findByWorkspaceIdAndUserId(1, 99))
                    .thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> workspaceService.removeMember(1, 20, 99),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateMemberRole()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateMemberRole()")
    class UpdateMemberRoleTests {

        @Test
        @DisplayName("should update member role successfully")
        void updateMemberRole_success() {
            UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
            req.setRole("ADMIN");

            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
            when(memberRepository.findByWorkspaceIdAndUserId(1, 20))
                    .thenReturn(Optional.of(regularMember));
            when(memberRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            WorkspaceMember result = workspaceService.updateMemberRole(1, 20, req, 10);

            assertThat(result.getRole()).isEqualTo(WorkspaceMember.Role.ADMIN);
        }

        @Test
        @DisplayName("should throw NOT_FOUND when member does not exist")
        void updateMemberRole_memberNotFound() {
            UpdateMemberRoleRequest req = new UpdateMemberRoleRequest();
            req.setRole("ADMIN");

            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
            when(memberRepository.findByWorkspaceIdAndUserId(1, 99))
                    .thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> workspaceService.updateMemberRole(1, 99, req, 10),
                    AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(ex.getMessage()).isEqualTo("Member not found");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getMembers() / isMember()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getMembers() and isMember()")
    class MemberQueryTests {

        @Test
        @DisplayName("getMembers() should return all members of a workspace")
        void getMembers_success() {
            when(workspaceRepository.findById(1)).thenReturn(Optional.of(workspace));
            when(memberRepository.findByWorkspaceId(1))
                    .thenReturn(List.of(ownerMember, regularMember));

            List<WorkspaceMember> result = workspaceService.getMembers(1);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("getMembers() should throw NOT_FOUND for unknown workspace")
        void getMembers_workspaceNotFound() {
            when(workspaceRepository.findById(99)).thenReturn(Optional.empty());

            AppException ex = catchThrowableOfType(
                    () -> workspaceService.getMembers(99), AppException.class);

            assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("isMember() should return true when user is a member")
        void isMember_true() {
            when(memberRepository.existsByWorkspaceIdAndUserId(1, 20)).thenReturn(true);
            assertThat(workspaceService.isMember(1, 20)).isTrue();
        }

        @Test
        @DisplayName("isMember() should return false when user is not a member")
        void isMember_false() {
            when(memberRepository.existsByWorkspaceIdAndUserId(1, 99)).thenReturn(false);
            assertThat(workspaceService.isMember(1, 99)).isFalse();
        }
    }
}