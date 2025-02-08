// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import com.pathplanner.lib.auto.AutoBuilder;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.DataLogManager;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.PowerDistribution;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.Constants.DriveConstants;
import frc.robot.Constants.OIConstants;
import frc.robot.subsystems.ArmSubsystem;
import frc.robot.subsystems.ElevatorSubsystem;
import frc.robot.subsystems.LEDSubsystem;
import frc.robot.subsystems.RollerSubsystem;
import frc.robot.subsystems.swervedrive.SwerveSubsystem;
import java.io.File;
import swervelib.SwerveInputStream;

/**
 * This class is where the bulk of the robot should be declared. Since Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of the robot (including
 * subsystems, commands, and button mappings) should be declared here.
 */
public class RobotContainer {
  // The robot's subsystems and commands are defined here...

  // First we do things that are in all Robots.
  private PowerDistribution pdp = new PowerDistribution();
  // The driver's controller
  private CommandXboxController driverController =
      new CommandXboxController(OIConstants.DRIVER_CONTROLLER_PORT);
  // The operator's controller
  private CommandXboxController operatorController =
      new CommandXboxController(OIConstants.OPERATOR_CONTROLLER_PORT);

  // Now all the subsystems.
  private final SwerveSubsystem drivebase =
      new SwerveSubsystem(new File(Filesystem.getDeployDirectory(), "swerve"));

  private final LEDSubsystem led = new LEDSubsystem();

  private final ArmSubsystem robotArm = new ArmSubsystem(ArmSubsystem.initializeHardware());

  private final ElevatorSubsystem robotElevator =
      new ElevatorSubsystem(ElevatorSubsystem.initializeHardware());

  private final RollerSubsystem robotRoller =
      new RollerSubsystem(RollerSubsystem.initializeHardware());

  /**
   * Converts driver input into a field-relative ChassisSpeeds that is controlled by angular
   * velocity.
   */
  SwerveInputStream driveAngularVelocity =
      SwerveInputStream.of(
              drivebase.getSwerveDrive(),
              () -> driverController.getLeftY() * -1,
              () -> driverController.getLeftX() * -1)
          .withControllerRotationAxis(() -> driverController.getRightX() * -1)
          .deadband(OIConstants.DEADBAND)
          .scaleTranslation(0.8)
          .allianceRelativeControl(DriveConstants.USE_ALLIANCE);

  // Applies deadbands and inverts controls because joysticks
  // are back-right positive while robot
  // controls are front-left positive
  // left stick controls translation
  // right stick controls the angular velocity of the robot
  Command driveFieldOrientedAngularVelocity =
      drivebase.driveFieldOriented(driveAngularVelocity).withName("Angular Velocity");

  /** Clone's the angular velocity input stream and converts it to a robotRelative input stream. */
  // This doesn't do what we want in 2025.1.1
  SwerveInputStream driveRobotOriented =
      driveAngularVelocity.copy().robotRelative(true).allianceRelativeControl(false);

  // Applies deadbands and inverts controls because joysticks
  // are back-right positive while robot
  // controls are front-left positive
  // left stick controls translation
  // right stick controls the angular velocity of the robot
  Command driveRobotOrientedAngularVelocity =
      drivebase.driveFieldOriented(driveRobotOriented).withName("Robot Oriented");

  // Commands to shift robot position at low speed using POV
  SwerveInputStream shiftForwardRobotOriented =
      SwerveInputStream.of(drivebase.getSwerveDrive(), () -> DriveConstants.POV_SPEED, () -> 0.0)
          .withControllerRotationAxis(() -> 0.0)
          .robotRelative(true)
          .allianceRelativeControl(false);

  Command shiftForward =
      drivebase.driveFieldOriented(shiftForwardRobotOriented).withName("Shift Forward");

  // Commands to shift robot position at low speed using POV
  SwerveInputStream shiftBackRobotOriented =
      SwerveInputStream.of(
              drivebase.getSwerveDrive(), () -> -1 * DriveConstants.POV_SPEED, () -> 0.0)
          .withControllerRotationAxis(() -> 0.0)
          .robotRelative(true)
          .allianceRelativeControl(false);

  Command shiftBack = drivebase.driveFieldOriented(shiftBackRobotOriented).withName("Shift Back");

  // Commands to shift robot position at low speed using POV
  SwerveInputStream shiftRightRobotOriented =
      SwerveInputStream.of(
              drivebase.getSwerveDrive(), () -> 0.0, () -> -1 * DriveConstants.POV_SPEED)
          .withControllerRotationAxis(() -> 0.0)
          .robotRelative(true)
          .allianceRelativeControl(false);

  Command shiftRight =
      drivebase.driveFieldOriented(shiftRightRobotOriented).withName("Shift Right");

  // Commands to shift robot position at low speed using POV
  SwerveInputStream shiftLeftRobotOriented =
      SwerveInputStream.of(drivebase.getSwerveDrive(), () -> 0.0, () -> DriveConstants.POV_SPEED)
          .withControllerRotationAxis(() -> 0.0)
          .robotRelative(true)
          .allianceRelativeControl(false);

  Command shiftLeft = drivebase.driveFieldOriented(shiftLeftRobotOriented).withName("Shift Left");

  private SendableChooser<Command> autoChooser;

  /** The container for the robot. Contains subsystems, OI devices, and commands. */
  public RobotContainer() {

    // Publish subsystem data including commands
    SmartDashboard.putData(drivebase);
    SmartDashboard.putData(robotArm);
    SmartDashboard.putData(robotElevator);
    SmartDashboard.putData(robotRoller);

    drivebase.setDefaultCommand(driveFieldOrientedAngularVelocity);

    // Setup the auto command chooser using the PathPlanner autos
    autoChooser = AutoBuilder.buildAutoChooser();
    SmartDashboard.putData(autoChooser);

    // Configure the button bindings
    configureButtonBindings();
  }

  /**
   * Use this method to define your button->command mappings. Buttons can be created by
   * instantiating a {@link GenericHID} or one of its subclasses ({@link
   * edu.wpi.first.wpilibj.Joystick} or {@link XboxController}), and then passing it to a {@link
   * edu.wpi.first.wpilibj2.command.button.JoystickButton}.
   */
  private void configureButtonBindings() {

    // ---------- Driver Controller ----------

    // Change drive type from field oriented to robot oriented, which is similar to tank drive, when
    // 'RB' is pressed on the driver's controller
    driverController.rightBumper().toggleOnTrue(driveRobotOrientedAngularVelocity);

    // Drive to a set position near the reef when 'B' is pressed on the driver's controller
    driverController
        .b()
        .whileTrue(
            drivebase.driveToPose(
                new Pose2d(new Translation2d(3.75, 2.65), Rotation2d.fromDegrees(60.0))));

    // Drives the robot slowly to a set position based on which of the pov buttons is pressed on the
    // driver's controller
    driverController.povUp().whileTrue(shiftForward);
    driverController.povDown().whileTrue(shiftBack);
    driverController.povRight().whileTrue(shiftRight);
    driverController.povLeft().whileTrue(shiftLeft);

    // ---------- Operator Controller ----------
    // Move the arm to the low position when the 'A' button is pressed on the operator's controller.
    operatorController
        .a()
        .onTrue(
            robotArm
                .moveToPosition(Constants.ArmConstants.ARM_FORWARD_POSITION_RADS)
                .andThen(robotArm::disable)
                .withName("Arm: Move to Forward Position"));

    // Move the arm to the high position when the 'B' button is pressed on the operator's
    // controller.
    operatorController
        .b()
        .onTrue(
            robotArm
                .moveToPosition(Constants.ArmConstants.ARM_BACK_POSITION_RADS)
                .andThen(robotArm::disable)
                .withName("Arm: Move to Back Position"));

    // Move the elevator to the low position when the 'A' button is pressed.
    operatorController
        .x()
        .onTrue(
            robotElevator
                .moveToPosition(Constants.ElevatorConstants.ELEVATOR_LOW_POSITION)
                .withName("Elevator: Move to Low Position"));

    // Move the elevator to the high position when the 'B' button is pressed.
    operatorController
        .y()
        .onTrue(
            robotElevator
                .moveToPosition(Constants.ElevatorConstants.ELEVATOR_HIGH_POSITION)
                .withName("Elevator: Move to High Position"));

    // Run the Roller forward when the right bumper is pressed.
    operatorController
        .rightBumper()
        .whileTrue(robotRoller.runForward().withName("Roller: Run Forward"));

    // Run the Roller in reverse when the left bumper is pressed.
    operatorController
        .leftBumper()
        .whileTrue(robotRoller.runReverse().withName("Roller: Run Reverse"));
  }

  /**
   * Disables all subsystems. This should be called on robot disable to prevent integrator windup in
   * subsystems with PID controllers. It also allows subsystems to setup disabled state so
   * simulation matches RoboRio behavior. Commands are canceled at the Robot level.
   */
  public void disableSubsystems() {
    robotArm.disable();
    robotRoller.disableRoller();
    DataLogManager.log("disableSubsystems");
  }

  /** Get the drive command from the drive subsystem. */
  public Command getTeleopDriveCommand() {
    return Commands.none();
  }

  /**
   * Use this to pass the autonomous command to the main {@link Robot} class.
   *
   * @return the command to run in autonomous
   */
  public Command getAutonomousCommand() {

    return autoChooser.getSelected();
  }

  /**
   * Use this to get the PDP for data logging.
   *
   * @return The PowerDistribution module.
   */
  public PowerDistribution getPdp() {
    return this.pdp;
  }

  /**
   * Use this to get the Arm Subsystem.
   *
   * @return a reference to the arm subsystem
   */
  public ArmSubsystem getArmSubsystem() {
    return robotArm;
  }

  /**
   * Use this to get the Elevator Subsystem.
   *
   * @return a reference to the Elevator Subsystem
   */
  public ElevatorSubsystem getElevatorSubsystem() {
    return robotElevator;
  }

  /**
   * Use this to get the Roller Subsystem.
   *
   * @return a reference to the Roller Subsystem
   */
  public RollerSubsystem getRollerSubsystem() {
    return robotRoller;
  }
}
